package com.cogkernal.semantic
package local

import com.cogkernal.semantic.SaferContext.{Categorization, Category, Dimension, MemoryPath}

trait SimpleControl extends ControlContext {
  Self: SaferContext =>

  override val decisionPolicy: DecisionPolicy = new DecisionPolicy {
    override def decide(proposed: Iterable[Commitment]): Option[Commitment] = {
      // could return multiple commitments
      proposed.headOption
    }
  }

  class SimpleLifecyclePolicy extends LifecyclePolicy {
    override def selectRootHeads(memories: Iterable[MemoryWithLineage]): Map[Dimension, MemoryWithLineage] = {
      for {
        dimension <- memories.map(_.lineage.dimension)
        latestRoot <- memoryRefs.resolve(MemoryScope(MemoryPath.root(dimension)), MemoryRef("latest"))
      } yield {
        dimension -> latestRoot
      }
    }.toMap

    override def selectRefineHeads(memory: MemoryWithLineage): Map[Categorization, Iterable[MemoryWithLineage]] = {
      // TODO should assign reference and resolve at categorization level and recursively
      val categories = memory.categorizations.toSeq.flatMap {
        categorization =>
          val categoryNames = memory.categories(categorization).keySet

          categoryNames.map(name => Category(name, categorization.categorization))
      }

      categories.groupBy(_.categorization).map {
        (categorization, categories) =>
          categorization ->
            categories.map {
              category =>
                memoryRefs.resolve(
                  MemoryScope(memory.lineage.memoryPath.nested(category)), MemoryRef("latest")
                )
            }.filter(_.nonEmpty).map(_.get)
      }
    }

    // assign latest reference for newly committed memories
    override def assignmentsAfterCommit(committed: Iterable[MemoryWithLineage]): Iterable[MemoryRefAssignment] = {
      committed.map {
        // the meaning of memory reference is determined by the policy
        memory => MemoryRefAssignment(MemoryScope(memory.lineage.memoryPath), MemoryRef("latest"), memory.lineage)
      }
    }

  }
  override val lifecyclePolicy: LifecyclePolicy = new SimpleLifecyclePolicy


  def resolve(dimension: String, category: (String, String)*): Option[MemoryWithLineage] = {
    memoryRefs.resolve(
      MemoryScope(
        MemoryPath(Dimension(dimension), category.map((name, cat) => Category(Category.Name(name), Categorization(cat)))),
      ),
      MemoryRef("latest")
    )
  }
}
