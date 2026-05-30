package com.cogkernal.semantic.employee

object EmployeeDomain {
  case class Employee(eid: Int, name: String, manager_id: Option[Int]) {
    var retrieval: Option[Int => Option[Employee]] = None
    def manager: Option[Employee] = {
      for {
        mid <- manager_id
        r <- retrieval
        retrieved <- r(mid)
      } yield {
        retrieved
      }
    }
    var managed: Set[Employee] = Set.empty
    var canMentor: Set[Employee] = Set.empty

    def merge(other: Employee): Employee = {
      if (eid != other.eid) throw new Exception(s"cannot merge different employees: $this, $other")
      val merged = Employee(eid, Option(name).getOrElse(other.name), Option(manager_id.getOrElse(other.manager_id.getOrElse(null))))
      merged.managed = managed ++ other.managed
      merged.canMentor = canMentor ++ other.canMentor
      merged.retrieval = (retrieval ++ other.retrieval).headOption
      merged
    }
    def level: Int = manager.map(_.level).getOrElse(0) + 1
    def data: Map[String, Any] = Map("eid" -> eid, "name" -> name, "manager_id" -> manager_id, "managed" -> managed.size, "level" -> level)
  }
}
