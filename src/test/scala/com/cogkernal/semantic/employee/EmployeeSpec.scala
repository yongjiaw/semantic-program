package com.cogkernal.semantic
package employee

import EmployeeDomain.Employee
import com.cogkernal.semantic.SaferContext.*
import com.cogkernal.semantic.local.*
import org.scalatest.funsuite.AnyFunSuite

class EmployeeSpec extends AnyFunSuite {
  test("manual program") {
    val runtime = new LocalSimpleRuntime[String, Employee]
    import runtime.*
    import runtime.Transition.*

    case class SourceAction(data: Iterable[Employee]) extends Source.Action {
      override def connect: LocalKeyedComputation[String, Employee] = LocalKeyedComputation(data.map(e => e.eid.toString -> e))
      override def reference: Reference = Reference("local data")
    }

    val source1 = Source.run(
      Dimension("Employee"),
      SourceAction(
        Seq(
          Employee(1, "CEO", None),
          Employee(2, "boss", Some(1)),
          Employee(3, "me", Some(2)),
          Employee(4, "colleague", Some(2)),
          Employee(5, "architect", Some(1))
        )
      )
    ).computeWithLinage
    // program id itself does not guarantee version uniqueness, it's the full lineage that defines a unique memory version
    // but here we use program id to manually bump the version
    // more complex control and lifecycle policies can be designed around custom versioning mechanisms
    // def employeeMostRecentVersion = memoryState.allAnchored.filter(_.lineage.dimension.name == "Employee").maxBy(_.lineage.action.id)
    var employeeMostRecentVersion: MemoryWithLineage = null
    // def mostRecentVersion(dimension: String) = memoryState.allAnchored.filter(_.lineage.dimension.name == dimension).maxBy(_.lineage.action.id)
    val anchorAction = new Anchor.Action {
      override def merge(left: Employee, right: Employee): Employee = left.merge(right)

      override def enrich(aligned: Employee): Employee = {
        // the serializable retrieval handle can point to the most recent version dynamically
        aligned.retrieval = Some(eid => employeeMostRecentVersion.memory.persisted.retrieval.retrieve(eid.toString))
        aligned
      }

      override def reference: Reference = Reference("merge employee and enrich")

      override def persistSpec: Any = null

      override def memorize(persisted: Persisted[String, Employee]): SimpleMemory = SimpleMemory(persisted)
    }

    employeeMostRecentVersion = Anchor.run(
      Reconcile(None, Set(source1)),
      anchorAction
    )

    assert(
      employeeMostRecentVersion.memory.persisted.sample().toSeq.map(_._2).sortBy(_.eid).map(_.data) ==
        Seq(
          Map("eid" -> 1, "name" -> "CEO", "manager_id" -> None, "managed" -> 0, "level" -> 1),
          Map("eid" -> 2, "name" -> "boss", "manager_id" -> Some(1), "managed" -> 0, "level" -> 2),
          Map("eid" -> 3, "name" -> "me", "manager_id" -> Some(2), "managed" -> 0, "level" -> 3),
          Map("eid" -> 4, "name" -> "colleague", "manager_id" -> Some(2), "managed" -> 0, "level" -> 3),
          Map("eid" -> 5, "name" -> "architect", "manager_id" -> Some(1), "managed" -> 0, "level" -> 2)
        )
    )

    val factorManage = new Factor.Action {
      override def decompose(data: Employee): Map[Dimension, Seq[(String, Employee)]] = {
        Map(
          Dimension("Employee") -> {
            val employee = data
            employee.manager_id.toSeq.map {
              mid =>
                // factor out manager and set the managed to current employee
                val manager = Employee(mid, null, None)
                manager.managed = Set(employee)
                mid.toString -> manager
            }
          }
        )
      }
      override def reference: Reference = Reference("factor")
    }
    val factored = Factor.run(employeeMostRecentVersion, factorManage)

    employeeMostRecentVersion = Anchor.run(
      // should use anchored source plus the factor
      Reconcile(Some(employeeMostRecentVersion), factored.values.toSet),
      anchorAction
    )

    assert(
      employeeMostRecentVersion.memory.persisted.sample().toSeq.map(_._2).sortBy(_.eid).map(_.data) ==
        Seq(
          Map("eid" -> 1, "name" -> "CEO", "manager_id" -> None, "managed" -> 2, "level" -> 1),
          Map("eid" -> 2, "name" -> "boss", "manager_id" -> Some(1), "managed" -> 2, "level" -> 2),
          Map("eid" -> 3, "name" -> "me", "manager_id" -> Some(2), "managed" -> 0, "level" -> 3),
          Map("eid" -> 4, "name" -> "colleague", "manager_id" -> Some(2), "managed" -> 0, "level" -> 3),
          Map("eid" -> 5, "name" -> "architect", "manager_id" -> Some(1), "managed" -> 0, "level" -> 2)
        )
    )

    val categorize = new Refine.Action {
      override def categorize(data: Employee): Category.Name = {
        if (data.managed.isEmpty) Category.Name("IC") else Category.Name("Management")
      }

      override def reference: Reference = Reference("categorize by track")

      override def persistSpec: Any = null

      override def memorize(persisted: Persisted[String, Employee]): SimpleMemory = SimpleMemory(persisted)
    }
    val categories = Refine.run(
      employeeMostRecentVersion -> Categorization("Track"),
      categorize
    )
    assert(categories(Category.Name("IC")).memory.persisted.sample().size == 3)
    assert(categories(Category.Name("Management")).memory.persisted.sample().size == 2)

    val source2 = Source.run(
      Dimension("Employee"),
      SourceAction(
        Seq(Employee(6, "minion", Some(3)))
      )
    ).computeWithLinage

    // update with new source as v3
    employeeMostRecentVersion = Anchor.run(Reconcile(Some(employeeMostRecentVersion), Set(source2)), anchorAction)

    // factor everything again, this is stateless recomputation with idempotency
    // even the new employee only affected one manager, the entire data is recomputed
    // if the backend is in streaming mode, the new value could be added to the same source using stateful aggregation
    val factored2 = Factor.run(employeeMostRecentVersion, factorManage)
    employeeMostRecentVersion = Anchor.run(
      Reconcile(Some(employeeMostRecentVersion), factored2.values.toSet), anchorAction
    )
    // recategorize on the new version
    val categories2 = Refine.run(
      employeeMostRecentVersion -> Categorization("Track"),
      categorize
    )
    assert(categories2(Category.Name("IC")).memory.persisted.sample().size == 3)
    assert(categories2(Category.Name("Management")).memory.persisted.sample().size == 3)

    val mentroship = Expand.run(
      (categories2(Category.Name("Management")), categories2(Category.Name("IC")), Dimension("Mentorship")),
      new Expand.Action {
        override def compose(left: Employee, right: Employee): Seq[(String, Employee)] = {
          {
            val manager = left
            val ic = right
            if (!manager.managed.contains(ic)) {
              val mentor = Employee(manager.eid, null, None)
              mentor.canMentor = Set(ic)
              // ideally should use relational representation so it can be projected to either direction
              // this simple demo used asymmetric representation on the manager as the intended projection target
              Seq(s"${left.eid},${right.eid}" -> mentor)
            } else Seq.empty
          }
        }

        override def reference: Reference = Reference("find mentorship")
      }
    )

    var mostRecentMentorship = Anchor.run(Reconcile(None, Set(mentroship)), anchorAction)

    assert(mostRecentMentorship.memory.persisted.sample().size == 6)
    mostRecentMentorship.memory.persisted.sample().foreach(println)

    val factorMentorship = Factor.run(
      mostRecentMentorship,
      new Factor.Action {
        override def decompose(data: Employee): Map[Dimension, Seq[(String, Employee)]] = {
          Map(
            Dimension("Employee") -> Seq(data.eid.toString -> data)
          )
        }

        override def reference: Reference = Reference("factor mentorship back to employee")
      }
    )

    employeeMostRecentVersion = Anchor.run(
      Reconcile(
      Some(employeeMostRecentVersion), factorMentorship.values.toSet), anchorAction)

    assert(employeeMostRecentVersion.memory.persisted.retrieval.retrieve("1").get.canMentor.map(_.eid) == Set(4, 6))
    assert(employeeMostRecentVersion.memory.persisted.retrieval.retrieve("2").get.canMentor.map(_.eid) == Set(5, 6))
    assert(employeeMostRecentVersion.memory.persisted.retrieval.retrieve("3").get.canMentor.map(_.eid) == Set(4, 5))
  }

  test("autonomous controlled program") {
    val control = new LocalSimpleRuntime[String, Employee] with SimpleControl {

      import Transition._
      override val elaborationPolicy: ElaborationPolicy = new ElaborationPolicy {
        override def factor(memoryWithLineage: MemoryWithLineage): Option[Transition.Factor.Action] = {
          if (memoryWithLineage.lineage.memoryPath == MemoryPath.root("Employee")) {
            val factor = new Factor.Action {
              override def decompose(data: Employee): Map[Dimension, Seq[(String, Employee)]] = {
                Map(
                  Dimension("Employee") -> {
                    val employee = data
                    employee.manager_id.toSeq.map {
                      mid =>
                        // factor out manager and set the managed to current employee
                        val manager = Employee(mid, null, None)
                        manager.managed = Set(employee)
                        mid.toString -> manager
                    }
                  }
                )
              }

              override def reference: Reference = Reference("factor")
            }
            Some(factor)
          }
          else None
        }

        override def expand(left: MemoryWithLineage, right: MemoryWithLineage): Map[Dimension, Transition.Expand.Action] = {
          if(
            left.lineage.memoryPath == MemoryPath(Dimension("Employee"), Seq(Category(Category.Name("Management"), Categorization("Track")))) &&
              right.lineage.memoryPath == MemoryPath(Dimension("Employee"), Seq(Category(Category.Name("IC"), Categorization("Track"))))
          ) {
            Map(
              Dimension("Mentorship") -> new Expand.Action {
                override def compose(left: Employee, right: Employee): Seq[(String, Employee)] = {
                  {
                    val manager = left
                    val ic = right
                    if (!manager.managed.contains(ic)) {
                      val mentor = Employee(manager.eid, null, None)
                      mentor.canMentor = Set(ic)
                      // ideally should use relational representation so it can be projected to either direction
                      // this simple demo used asymmetric representation on the manager as the intended projection target
                      Seq(s"${left.eid},${right.eid}" -> mentor)
                    } else Seq.empty
                  }
                }

                override def reference: Reference = Reference("find mentorship across manager and IC not being direct report")
              }
            )
          } else Map.empty
        }
      }
      override val proposalPolicy: ProposalPolicy = new ProposalPolicy {

        override def anchor(reconcile: Transition.Reconcile): Transition.Anchor.Action = {
          new Transition.Anchor.Action {
            override def merge(left: Employee, right: Employee): Employee = left.merge(right)

            override def enrich(aligned: Employee): Employee = {
              // the serializable retrieval handle can point to the most recent version dynamically
              aligned.retrieval = Some(eid => resolve("Employee").get.memory.persisted.retrieval.retrieve(eid.toString))
              aligned
            }

            override def reference: Reference = Reference("merge employee and enrich")

            override def persistSpec: Any = null

            override def memorize(persisted: Persisted[String, Employee]): SimpleMemory = SimpleMemory(persisted)
          }
        }

        override def refine(memoryWithLineage: MemoryWithLineage): Map[Categorization, Refine.Action] = {
          if (memoryWithLineage.lineage.dimension.name == "Employee") {
            Map(
              Categorization("Track") -> new Refine.Action {
                override def categorize(data: Employee): Category.Name = {
                  if (data.managed.isEmpty) Category.Name("IC") else Category.Name("Management")
                }

                override def reference: Reference = Reference("categorize by track")

                override def persistSpec: Any = null

                override def memorize(persisted: Persisted[String, Employee]): SimpleMemory = SimpleMemory(persisted)
              }
            )
          }
          else {
            Map.empty
          }

        }
      }
    }

    import control.*
    import control.Transition.*

    case class SourceAction(data: Iterable[Employee]) extends Source.Action {
      override def connect: LocalKeyedComputation[String, Employee] = LocalKeyedComputation(data.map(e => e.eid.toString -> e))

      override def reference: Reference = Reference(s"local data ${data.size}")
    }

    def simpleLineage(lineage: Lineage): String = {
      s"${lineage.getClass.getSimpleName}(${
        lineage match {
          case memorize: Lineage.Memorize => memorize.memoryPath
          case compute: Lineage.Compute => compute.dimension
        }
      },${lineage.action.id})"
    }

    def simpleCommitment(commitment: Commitment): String = {
      commitment match {
        case anchor: Commitment.Anchor =>
          s"Anchor(${anchor.reconcile.head.map(_.lineage).map(simpleLineage) -> anchor.reconcile.computes.map(_.lineage).map(simpleLineage)})"

        case refine: Commitment.Refine =>
          s"Refine(${refine.categorization -> simpleLineage(refine.parent.lineage)})"
      }
    }

    def details(decisionTrace: DecisionTrace): Seq[(String, Any)] = {
      Seq(
        "roots" -> decisionTrace.selectedStates.rootMemories.keySet,
        "refines" -> decisionTrace.selectedStates.refineMemories.map(_.lineage.memoryPath).toSet,
        s"elaborations: ${decisionTrace.elaborations.size}" -> decisionTrace.elaborations.map(_.lineage).map(simpleLineage),
        s"proposals: ${decisionTrace.proposals.size}" -> decisionTrace.proposals.map(simpleCommitment),
        s"decision" -> decisionTrace.decision.map(simpleCommitment),
        "result" -> decisionTrace.result
      )
    }
    def summary(decisionTrace: DecisionTrace): Any = {
      Map(
        "roots" -> decisionTrace.selectedStates.rootMemories.size,
        "refines" -> decisionTrace.selectedStates.refineMemories.size,
        "elaborations " -> decisionTrace.elaborations.size,
        "proposals " -> decisionTrace.proposals.size
      )
    }

    control.source(
      Dimension("Employee"),
      SourceAction(
        Seq(
          Employee(1, "CEO", None),
          Employee(2, "boss", Some(1)),
          Employee(3, "me", Some(2)),
          Employee(4, "colleague", Some(2)),
          Employee(5, "architect", Some(1))
        )
      )
    )
    details(control.cycle()).foreach(println)
    assert(
      resolve("Employee").get.memory.persisted.sample().toSeq.map(_._2).sortBy(_.eid).map(_.data) ==
        Seq(
          Map("eid" -> 1, "name" -> "CEO", "manager_id" -> None, "managed" -> 0, "level" -> 1),
          Map("eid" -> 2, "name" -> "boss", "manager_id" -> Some(1), "managed" -> 0, "level" -> 2),
          Map("eid" -> 3, "name" -> "me", "manager_id" -> Some(2), "managed" -> 0, "level" -> 3),
          Map("eid" -> 4, "name" -> "colleague", "manager_id" -> Some(2), "managed" -> 0, "level" -> 3),
          Map("eid" -> 5, "name" -> "architect", "manager_id" -> Some(1), "managed" -> 0, "level" -> 2)
        )
    )
    details(control.cycle()).foreach(println)

    assert(
      resolve("Employee").get.memory.persisted.sample().toSeq.map(_._2).sortBy(_.eid).map(_.data) ==
        Seq(
          Map("eid" -> 1, "name" -> "CEO", "manager_id" -> None, "managed" -> 2, "level" -> 1),
          Map("eid" -> 2, "name" -> "boss", "manager_id" -> Some(1), "managed" -> 2, "level" -> 2),
          Map("eid" -> 3, "name" -> "me", "manager_id" -> Some(2), "managed" -> 0, "level" -> 3),
          Map("eid" -> 4, "name" -> "colleague", "manager_id" -> Some(2), "managed" -> 0, "level" -> 3),
          Map("eid" -> 5, "name" -> "architect", "manager_id" -> Some(1), "managed" -> 0, "level" -> 2)
        )
    )

    details(control.cycle()).foreach(println)

    assert(resolve("Employee", "IC" -> "Track").get.memory.persisted.sample().size == 3)
    assert(resolve("Employee", "Management" -> "Track").get.memory.persisted.sample().size == 2)

    details(control.cycle()).foreach(println)

    // (2,5), (1,3), (1,4)
    assert(resolve("Mentorship").get.memory.persisted.sample().size == 3)
    
    // no more actions to take
    assert(control.cycle().decision.isEmpty)
    control.source(
      Dimension("Employee"),
      SourceAction(
        Seq(
          Employee(6, "minion", Some(3))
        )
      )
    )
    
    details(control.cycle()).foreach(println)
    resolve("Employee").get.memory.persisted.sample().toSeq.map(_._2).sortBy(_.eid).map(_.data).foreach(println)
    assert(resolve("Employee").get.memory.persisted.sample().size == 6)

    details(control.cycle()).foreach(println)
    resolve("Employee").get.memory.persisted.sample().toSeq.map(_._2).sortBy(_.eid).map(_.data).foreach(println)
    
    details(control.cycle()).foreach(println)
    // resolve("Employee").get.memory.persisted.sample().foreach(println)
    assert(resolve("Employee", "IC" -> "Track").get.memory.persisted.sample().size == 3)
    assert(resolve("Employee", "Management" -> "Track").get.memory.persisted.sample().size == 3)
    
    details(control.cycle()).foreach(println)
  }
}
