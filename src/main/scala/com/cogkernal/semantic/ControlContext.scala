package com.cogkernal.semantic

import com.cogkernal.semantic.SaferContext.Categorization
import SaferContext.*

/**
 * This control structure is inspired by Soar decision cycles which decomposes the complex decision into
 * a few bounded decision surfaces, each with a control policy
 */
trait ControlContext {
  Self: SaferContext =>

  import Transition.*

  def source(dimension: Dimension, action: Transition.Source.Action): ExternalSource = {
    externalSource.getOrElseUpdate(
      Lineage.Source(dimension, action.reference),
      Transition.Source.run(dimension, action)
    )
  }

  case class MemoryScope(
                          path: MemoryPath,
                          namespace: String = "default"
                        )

  case class MemoryRef(name: String)

  case class MemoryRefAssignment(
                                  scope: MemoryScope,
                                  ref: MemoryRef,
                                  lineage: Lineage.Memorize
                                )

  class MemoryRefIndex {
    private val refs =
      collection.mutable.Map.empty[(MemoryScope, MemoryRef), Lineage.Memorize]

    def assign(a: MemoryRefAssignment): Unit = refs.update((a.scope, a.ref), a.lineage)

    def resolve(scope: MemoryScope, ref: MemoryRef): Option[MemoryWithLineage] = {
      refs.get((scope, ref)).flatMap {
        lineage =>
          val memoryVersionPath = lineage.memoryVersionPath
          val root = committedState.get(memoryVersionPath.anchor)
          // TODO support recursive resolving
          memoryVersionPath.category.headOption match {
            case None => root
            case Some(categoryVersion) =>
              root.map(
                memory => memory.categories(categoryVersion.categorizationVersion)(categoryVersion.name)
              )
          }
      }
    }
  }

  val memoryRefs = new MemoryRefIndex

  // private mutable state
  private val externalSource = collection.mutable.Map.empty[Lineage.Source, ExternalSource]
  private val committedState = collection.mutable.Map.empty[Lineage.Memorize, MemoryWithLineage]

  sealed trait Commitment {
    def commit(): Iterable[MemoryWithLineage]

    // proposal may generate already applied path, which should be excluded
    // the entire state evolution is monotonic
    def isNewContent: Boolean

  }

  object Commitment {
    case class Anchor(reconcile: Reconcile, action: Transition.Anchor.Action) extends Commitment {
      override def isNewContent: Boolean = { // validate the proposed action, it should not already exist
        val lineage = Lineage.Anchor(reconcile.align, action.reference)
        !committedState.contains(lineage)
      }

      override def commit(): Iterable[MemoryWithLineage] = {
        val result = Transition.Anchor.run(reconcile, action)
        committedState.put(result.lineage, result)
        Seq(result)
      }
    }

    case class Refine(parent: MemoryWithLineage, categorization: Categorization, action: Transition.Refine.Action) extends Commitment {
      override def isNewContent: Boolean = {
        // the same categorization regardless of version should only appear once down the hierarchy
        !parent.lineage.memoryPath.category.exists(_.categorization == categorization) && {
          // the same categorization version should not be among siblings
          !parent.categorizations.contains(CategorizationVersion(categorization, action.reference))
        }
      }

      override def commit(): Iterable[MemoryWithLineage] = {
        // Safer.Refine.run(parent -> categorization, action).values
        parent.refine(parent.lineage, categorization, action).values
      }
    }
  }


  case class SelectedStates(
                             rootMemories: Map[Dimension, MemoryWithLineage],
                             refineMemories: Iterable[MemoryWithLineage],
                             externalSources: Iterable[ExternalSource]
                           ) {
    def memories: Iterable[MemoryWithLineage] = rootMemories.values ++ refineMemories
  }

  trait LifecyclePolicy {

    // the candidate pool has multiple versions
    // only one version per dimension is eligible for evolve per cycle
    // deprecation, rollback and branching logic can be injected at this layer
    protected def selectRootHeads(
                                   memories: Iterable[MemoryWithLineage]
                                 ): Map[Dimension, MemoryWithLineage]

    protected def selectRefineHeads(
                                     memory: MemoryWithLineage
                                   ): Map[Categorization, Iterable[MemoryWithLineage]]

    protected def selectSources(
                                 selectedRoots: Map[Dimension, MemoryWithLineage],
                                 externalSources: Iterable[ExternalSource],
                               ): Iterable[ExternalSource] = {
      externalSources
    }

    def assignmentsAfterCommit(
                                committed: Iterable[MemoryWithLineage]
                              ): Iterable[MemoryRefAssignment]

    final def selectHeads(
                           memories: Iterable[MemoryWithLineage],
                           externalSources: Iterable[ExternalSource]
                         ): SelectedStates = {

      val rootHeads: Map[Dimension, MemoryWithLineage] = selectRootHeads(memories)

      def eligibleCategories(memory: MemoryWithLineage): Set[MemoryWithLineage] = {
        // return one version per categorization, and recursively find eligible refinements
        val selected = selectRefineHeads(memory).values.flatten.toSet
        selected ++ selected.flatMap(eligibleCategories)
      }

      val categories: Set[MemoryWithLineage] = rootHeads.values.flatMap(eligibleCategories).toSet

      // new sources that not already used by the selected heads
      val newSources = externalSources.filter {
        source =>
          rootHeads.get(source.dimension) match {
            case Some(memory) => !memory.lineage.dependOn(source.lineage)
            case None => true
          }
      }

      SelectedStates(rootHeads, categories, selectSources(rootHeads, newSources))
    }
  }

  val lifecyclePolicy: LifecyclePolicy

  trait ElaborationPolicy {

    def factor(memoryWithLineage: MemoryWithLineage): Option[Transition.Factor.Action]

    // more efficient matching algorithm should search the graph actively instead of iterating through all candidates
    def expand(left: MemoryWithLineage, right: MemoryWithLineage): Map[Dimension, Transition.Expand.Action]

    def canExpand(a: MemoryWithLineage, b: MemoryWithLineage): Boolean = {
      (a.lineage.dimension != b.lineage.dimension) || {
        // if same dimension, they must not include each other
        // they must be different categories under the same categorization
        // different categorization should not be combined
        // the only exception the same memory can do a self expansion
        (a.lineage, b.lineage) match {
          case (refineA: Lineage.Refine, refineB: Lineage.Refine) =>
            // TODO make this more general for arbitrary nested categories
            refineA.category.categorization == refineB.category.categorization && refineA.category.name != refineB.category.name
          case _ => false
        }
      }
    }

  }

  val elaborationPolicy: ElaborationPolicy

  // all valid changes for next step
  // just go through all the memories and propose new computes for factor and expand
  private def elaborate(heads: Iterable[MemoryWithLineage]): Iterable[ComputeWithLinage] = {
    // factor for each root head and nested refinement
    // each factor will produce at most one compute per dimension, they will be collected and reconciled
    val factorResult: Iterable[ComputeWithLinage] =
      for {
        memory <- heads
        factorAction <- elaborationPolicy.factor(memory).toSeq
        (toDimension, result) <- Transition.Factor.run(memory, factorAction)
      } yield {
        result
      }

    // self joint
    val expand1: Iterable[ComputeWithLinage] = for {
      memory <- heads
      (dimension, expand) <- elaborationPolicy.expand(memory, memory)
    } yield {
      Transition.Expand.run((memory, memory, dimension), expand)
    }
    // for expanding different memories, pairs of the same dimension must be independent
    // if they are from the same dimension, they should come from different categories of the same categorization
    val expand2: Iterable[ComputeWithLinage] = for {
      left <- heads
      right <- heads if elaborationPolicy.canExpand(left, right)
      (dimension, expand) <- elaborationPolicy.expand(left, right)
    } yield {
      Transition.Expand.run((left, right, dimension), expand)
    }

    factorResult ++ expand1 ++ expand2
  }

  trait ProposalPolicy {
    def reconcile(head: Option[MemoryWithLineage], computes: Set[ComputeWithLinage]): Seq[Reconcile] = {
      if (head.isEmpty) {
        Seq(Reconcile(head, computes))
      }
      else {
        val anchor = head.get.lineage.memoryVersionPath.anchor
        val incorporatedComputes = anchor.align.all.map(_.action)
        val duplicates = computes.filter {
          compute => incorporatedComputes.contains(compute.lineage.action)
        }
        // need to compare the new information since last time applying the same action
        // if the head has incorporated new information for the action, the same action is applicable again
        // need a method to find the delta of the source
        val remaining = computes.filter(c => !duplicates.contains(c))
        println(s"reconciling head ${head.get.lineage.memoryPath}: incorporated=${incorporatedComputes}, duplicates=${duplicates.map(_.lineage.action)}, remaining=${remaining.map(_.lineage.action)}")
        if (remaining.nonEmpty) Seq(Reconcile(head, remaining)) else Seq.empty
        // a reasonable default is to create one version of reconcile
        // more complex strategy can create branches here

      }
    }

    // this is where things can branch
    def anchor(reconcile: Reconcile): Transition.Anchor.Action

    def refine(memoryWithLineage: MemoryWithLineage): Map[Categorization, Refine.Action]
  }

  val proposalPolicy: ProposalPolicy

  case class ElaboratedState(
                              selectedState: SelectedStates,
                              elaboratedComputes: Iterable[ComputeWithLinage]
                            ) {
    // align computes, sources and root memories for reconciliation
    def dimensionAlignments: Map[Dimension, (Option[MemoryWithLineage], Set[ComputeWithLinage])] = {
      (selectedState.externalSources.map(_.computeWithLinage) ++ elaboratedComputes).groupBy(_.lineage.dimension).map {
        case (dimension, computes) => dimension -> (selectedState.rootMemories.get(dimension) -> computes.toSet)
      }
    }
  }

  // this is based on general eligibility of memory versions and data sources
  // all eligible reconciliations will be proposed
  private def propose(elaboratedState: ElaboratedState): Iterable[Commitment] = {

    val proposedAnchors = elaboratedState.dimensionAlignments.flatMap {
      case (dimension, (memory, computes)) => proposalPolicy.reconcile(memory, computes)
        .map(r => Commitment.Anchor(r, proposalPolicy.anchor(r)))
    }

    val proposedRefines = elaboratedState.selectedState.memories.flatMap {
      memory => proposalPolicy.refine(memory).map(
        (categorization, action) => Commitment.Refine(memory, categorization, action)
      )
    }
    proposedAnchors ++ proposedRefines
  }

  trait DecisionPolicy {
    // should only allow at most one reconciliation action per cycle, this moves the state of the memory
    // everything else is just elaborations
    def decide(proposed: Iterable[Commitment]): Option[Commitment]
  }


  // policy could be dynamic
  val decisionPolicy: DecisionPolicy

  case class DecisionTrace(
                            sources: Iterable[ExternalSource],
                            initialState: Iterable[MemoryWithLineage],
                            selectedStates: SelectedStates,
                            elaborations: Iterable[ComputeWithLinage],
                            proposals: Iterable[Commitment],
                            decision: Option[Commitment],
                            result: Iterable[MemoryRefAssignment]
                          )

  // decision cycle has multiple phases
  // elaborate -> propose -> decide -> commit(reconcile or refine) -> repeat
  final def cycle(): DecisionTrace = {

    val selectedState = lifecyclePolicy.selectHeads(committedState.values, externalSource.values)
    val elaboratedComputes = elaborate(selectedState.memories) // elaboration
    val proposals = propose( // proposal
      ElaboratedState(
        selectedState = selectedState,
        elaboratedComputes = elaboratedComputes
      )).filter(_.isNewContent)

    // decision and commit
    val decision = decisionPolicy.decide(proposals)

    val result = decision.map(
        decided => {
          val newMemories = decided.commit()
          val newMemoryRefAssignments = lifecyclePolicy.assignmentsAfterCommit(newMemories)
          newMemoryRefAssignments.foreach(memoryRefs.assign)
          newMemoryRefAssignments
        }
      )
      // TODO plugin reference policy to assign scoped memory references such as latest, active, stable, candidate, etc
      .getOrElse(Iterable.empty)

    DecisionTrace(
      externalSource.values,
      committedState.values,
      selectedState,
      elaboratedComputes,
      proposals,
      decision,
      result
    )
  }
}
