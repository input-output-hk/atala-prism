package io.iohk.cvp.cmanager.repositories

import java.time.LocalDate

import io.iohk.cvp.cmanager.models.requests.CreateStudent
import io.iohk.cvp.cmanager.models.{Issuer, IssuerGroup, Student}
import io.iohk.cvp.cmanager.repositories.common.CManagerRepositorySpec
import io.iohk.cvp.cmanager.repositories.common.DataPreparation._
import org.scalatest.EitherValues._
import org.scalatest.OptionValues._

class StudentsRepositorySpec extends CManagerRepositorySpec {
  lazy val repository = new StudentsRepository(database)

  "create" should {
    "create a new student" in {
      val issuer = createIssuer("Issuer-1").id
      val group = createIssuerGroup(issuer, IssuerGroup.Name("Grp 1"))
      val request = CreateStudent(issuer, "uid", "Dusty Here", "d.here@iohk.io", LocalDate.now(), group.name)
      val result = repository.create(request).value.futureValue
      val student = result.right.value
      student.groupName must be(group.name)
      student.fullName must be(request.fullName)
      student.universityAssignedId must be(request.universityAssignedId)
      student.email must be(request.email)
      student.admissionDate must be(request.admissionDate)
    }
  }

  "getBy" should {
    "return the first students" in {
      val issuer = createIssuer("Issuer X").id
      val credA = createStudent(issuer, "A")
      val credB = createStudent(issuer, "B")
      val credC = createStudent(issuer, "C")

      val result = repository.getBy(issuer, 2, None).value.futureValue.right.value
      result.toSet must be(Set(credA, credB))
    }

    "paginate by the last seen student" in {
      val issuer = createIssuer("Issuer X").id
      val credA = createStudent(issuer, "A")
      val credB = createStudent(issuer, "B")
      val credC = createStudent(issuer, "C")
      val credD = createStudent(issuer, "D")

      val first = repository.getBy(issuer, 2, None).value.futureValue.right.value
      val result = repository.getBy(issuer, 1, first.lastOption.map(_.id)).value.futureValue.right.value
      result.toSet must be(Set(credC))
    }
  }

  "generateToken" should {
    "update the student to set the status and token" in {
      val issuer = createIssuer("tokenizer").id
      val student = createStudent(issuer, "token")
      val result = repository.generateToken(issuer, student.id).value.futureValue
      val token = result.right.value

      val newStudent = repository.find(issuer, student.id).value.futureValue.right.value.value
      newStudent.connectionStatus must be(Student.ConnectionStatus.ConnectionMissing)
      newStudent.connectionToken.value must be(token)
    }
  }

  private def createStudent(issuer: Issuer.Id, tag: String = "", groupMaybe: Option[IssuerGroup] = None): Student = {
    val group = groupMaybe.getOrElse {
      createIssuerGroup(issuer, IssuerGroup.Name(s"Grp 1 - $tag"))
    }
    val request = CreateStudent(
      issuer = issuer,
      universityAssignedId = s"uid $tag",
      fullName = s"Atala Prism $tag".trim,
      email = "test@iohk.io",
      admissionDate = LocalDate.now(),
      groupName = group.name
    )

    val result = repository.create(request).value.futureValue
    result.right.value
  }
}
