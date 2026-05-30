package com.cogkernal.semantic

import com.cogkernal.semantic.SaferContext.Lineage.Align
import SaferContext.*

object SaferContext {

  case class Dimension(name: String)

  case class Categorization(name: String)
  case class Category(name: Category.Name, categorization: Categorization)

  object Category {
    case class Name(name: String)
  }

  case class CategorizationVersion(categorization: Categorization, versionRef: Reference)

  case class CategoryVersion(name: Category.Name, categorizationVersion: CategorizationVersion) {
    def category: Category = Category(name, categorizationVersion.categorization)
  }

  case class MemoryPath(dimension: Dimension, category: Seq[Category]) {
    def nested(cat: Category): MemoryPath = this.copy(category = category :+ cat)
   }

  case class MemoryVersionPath(anchor: Lineage.Anchor, category: Seq[CategoryVersion]) {
    def memoryPath: MemoryPath = MemoryPath(anchor.dimension, category.map(_.category))
  }

  object MemoryPath {
    def root(dimension: Dimension): MemoryPath = MemoryPath(dimension, Seq.empty)
    def root(name: String): MemoryPath = MemoryPath.root(Dimension(name))
  }

  trait Action {
    def reference: Reference
  }
  case class Reference(id: String)

  sealed trait Lineage {
    def dimension: Dimension
    def action: Reference
    def dependencies: Set[Lineage]
    def dependOn(lineage: Lineage): Boolean = {
      dependencies.contains(lineage) || dependencies.exists(_.dependOn(lineage))
    }
    def asProducer: Lineage.Compute
  }

  object Lineage {

    sealed trait Compute extends Lineage {
      override def asProducer: Compute = this
    }

    sealed trait Memorize extends Lineage {
      def asProducer: Compute = Source(dimension, action, Some(this))
      def memoryVersionPath: MemoryVersionPath = this match {
        case anchor: Anchor => MemoryVersionPath(anchor, Seq.empty)
        case refine: Refine => 
          val parentPath = refine.parent.memoryVersionPath
          parentPath.copy(category = parentPath.category :+  refine.categoryVersion)
      }
      def memoryPath: MemoryPath = memoryVersionPath.memoryPath
    }

    // Source, Factor, Expand produce derived data that can have conflict
    case class Source(dimension: Dimension, action: Reference, upstream: Option[Lineage.Memorize] = None) extends Compute {
      override def dependencies: Set[Lineage] = upstream.toSet
    }
    
    case class Align(head: Option[Lineage.Memorize], computes: Set[Lineage.Compute]) {
      val all: Set[Lineage.Compute] = computes ++ head.map(_.asProducer)
      require(all.nonEmpty)
      val dimension: Dimension = all.head.dimension
      require(all.forall(_.dimension == dimension), s"inconsistent dimensions: head=${head.map(_.dimension)}, computes=${computes.map(_.dimension)}")
    }

    // Anchor is the single point of reconciliation to consolidate multiple upstreams
    case class Anchor(align: Align, action: Reference) extends Memorize {
      override def dependencies: Set[Lineage] = align.all.map(l => l:Lineage)

      override def dimension: Dimension = align.dimension
    }

    case class Factor(from: Memorize, dimension: Dimension, action: Reference) extends Compute {
      override def dependencies: Set[Lineage] = Set(from)
    }

    case class Expand(left: Memorize, right: Memorize, dimension: Dimension, action: Reference) extends Compute {
      override def dependencies: Set[Lineage] = Set(left, right)
    }

    // categorization can be recursive
    case class Refine(parent: Memorize, category: Category, action: Reference) extends Memorize {
      override def dimension: Dimension = parent.dimension
      override def dependencies: Set[Lineage] = Set(parent)
      def categoryVersion: CategoryVersion = CategoryVersion(category.name, CategorizationVersion(category.categorization, action))
    }

  }
}

trait SaferContext {
  Self: ComputationContext & PersistenceContext =>

  type Key
  type Repr
  type Memory <: Memorized

  abstract class Memorized {
    val persisted: Persisted[Key, Repr]
  }
  
  case class MemoryWithLineage(memory: Memory, lineage: Lineage.Memorize, parent: Option[MemoryWithLineage] = None) {
    private val _categorizations = collection.mutable.Map.empty[CategorizationVersion, Map[Category.Name, MemoryWithLineage]]
    final def refine(lineage: Lineage.Memorize, categorization: Categorization, action: Transition.Refine.Action): Map[Category.Name, MemoryWithLineage] = {
      val categorizationVersion = CategorizationVersion(categorization, action.reference)
      _categorizations.getOrElseUpdate(
        categorizationVersion,
        memory.persisted.dataPlan.split((k, v) => action.categorize(v)).map {
          case (category, compute) =>
            category -> MemoryWithLineage(
              action.memorize(persist(action.persistSpec, compute)),
              Lineage.Refine(lineage, Category(category, categorization), action.reference),
              parent = Some(this)
            )
        }
      )
    }

    final def categorizations: Set[CategorizationVersion] = _categorizations.keySet.toSet

    final def categories(categorizationVersion: CategorizationVersion): Map[Category.Name, MemoryWithLineage] =
      _categorizations.getOrElse(categorizationVersion, Map.empty)
  }

  case class ExternalSource(dimension: Dimension, action: Reference, compute: KC[Key, Repr]) {
    def computeWithLinage: ComputeWithLinage = ComputeWithLinage(compute, Lineage.Source(dimension, action, None))

    def lineage: Lineage.Source = Lineage.Source(dimension, action, None)
  }
  case class ComputeWithLinage(compute: KC[Key, Repr], lineage: Lineage.Compute)
  
  sealed trait Transition[In, Out] {
    type A
    def run(condition: In, action: A): Out
  }

  object Transition {
    sealed trait Compute extends Action

    sealed trait Memorize extends Action {
      // generic persistence layer can support retention, storage type, etc.
      def persistSpec: PersistSpec

      //additional custom semantics to the memory such as predictive model training and compression
      // create custom memory wrapper of the persisted generic
      def memorize(persisted: Persisted[Key, Repr]): Memory
    }

    object Source extends Transition[Dimension, ExternalSource] {
      trait Action extends Compute {
        def connect: KC[Key, Repr]
      }

      override type A = Action
      
      def run(dimension: Dimension, action: Action): ExternalSource = {
        ExternalSource(dimension, action.reference, action.connect)
      }
    }

    // only one memory head is allowed
    case class Reconcile(head: Option[MemoryWithLineage], computes: Set[ComputeWithLinage]) {
      // if reconciliation can happen at refinement level, 
      // the process need to make sure the same source is not reconciled multiple times at different refinement level 
      // head.foreach(m => require(m.parent.isEmpty))
      
      val lineage: Set[Lineage.Compute] = head.toSet.map(_.lineage.asProducer) ++ computes.map(_.lineage)
      val align: Align = Align(head.map(_.lineage), computes.map(_.lineage))
      val source: Seq[KC[Key, Repr]] = head.toSeq.map(_.memory.persisted.dataPlan) ++ computes.map(_.compute)
      val toDimension: Dimension = lineage.head.dimension
      require(lineage.forall(_.dimension == toDimension))
      
    }
    object Anchor extends Transition[Reconcile, MemoryWithLineage] {
      trait Action extends Memorize {
        def merge(left: Repr, right: Repr): Repr

        def enrich(aligned: Repr): Repr
      }

      type A = Action

      override def run(reconcile: Reconcile, action: Action): MemoryWithLineage = {
        MemoryWithLineage(
          lineage = Lineage.Anchor(reconcile.align, action.reference),
          memory = action.memorize(
            Self.persist(
              action.persistSpec,
              // if the head is persisted as streaming source, the spec with union can feed new data in the existing head,
              // new head will be persisted as a reference, there can be different strategies customized at compute and persistence layers
              reconcile.source
                .reduce((a, b) => a.union(b))
                .reduce(action.merge)
                .map(action.enrich)
            )
          )
        )
      }
    }

    object Factor extends Transition[MemoryWithLineage, Map[Dimension, ComputeWithLinage]] {
      trait Action extends Compute {
        def decompose(data: Repr): Map[Dimension, Seq[(Key, Repr)]]
      }

      type A = Action

      def run(input: MemoryWithLineage, action: Action): Map[Dimension, ComputeWithLinage] = {
        input.memory.persisted.dataPlan
          .flatMap((k, v) => action.decompose(v))
          .split((toDimension, data) => toDimension)
          .map {
            case (toDimension, data) =>
              toDimension ->
                ComputeWithLinage(
                  lineage = Lineage.Factor(input.lineage, toDimension, action.reference),
                  compute = data.flatMap((toDimension, data) => data)
                )
          }
      }
    }

    object Expand extends Transition[(MemoryWithLineage, MemoryWithLineage, Dimension), ComputeWithLinage] {
      trait Action extends Compute {
        def compose(left: Repr, right: Repr): Seq[(Key, Repr)]
      }

      type A = Action

      def run(relation: (MemoryWithLineage, MemoryWithLineage, Dimension), action: Action): ComputeWithLinage = {
        val (left, right, toDimension) = relation
        ComputeWithLinage(
            lineage = Lineage.Expand(left.lineage, right.lineage, toDimension, action.reference),
            compute = join(left.memory.persisted.dataPlan, right.memory.persisted.dataPlan)
              .flatMap((k, v) => action.compose(v._1, v._2))
          )
      }
    }

    object Refine extends Transition[(MemoryWithLineage, Categorization), Map[Category.Name, MemoryWithLineage]] {
      trait Action extends Memorize {
        def categorize(data: Repr): Category.Name
      }

      type A = Action

      override def run(parentCategorization: (MemoryWithLineage, Categorization), action: Action): Map[Category.Name, MemoryWithLineage] = {
        val (parent, categorization) = parentCategorization
        parent.refine(parent.lineage, categorization, action)
      }
    }
  }

}
