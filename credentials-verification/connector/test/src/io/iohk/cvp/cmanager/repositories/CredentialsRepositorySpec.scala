package io.iohk.cvp.cmanager.repositories

import java.time.LocalDate

import io.iohk.cvp.cmanager.models.requests.CreateCredential
import io.iohk.cvp.cmanager.repositories.common.CManagerRepositorySpec
import io.iohk.cvp.cmanager.repositories.common.DataPreparation._
import org.scalatest.EitherValues._

class CredentialsRepositorySpec extends CManagerRepositorySpec {

  lazy val repository = new CredentialsRepository(database)
  lazy val studentsRepository = new StudentsRepository(database)

  "create" should {
    "create a new credential" in {
      val issuer = createIssuer("Issuer-1")
      val student = createStudent(issuer.id, "Student 1")
      val request = CreateCredential(
        issuedBy = issuer.id,
        studentId = student.id,
        title = "Major IN Applied Blockchain",
        enrollmentDate = LocalDate.now(),
        graduationDate = LocalDate.now().plusYears(5),
        groupName = "Computer Science"
      )

      val result = repository.create(request).value.futureValue
      val credential = result.right.value
      credential.enrollmentDate must be(request.enrollmentDate)
      credential.graduationDate must be(request.graduationDate)
      credential.issuedBy must be(request.issuedBy)
      credential.studentId must be(request.studentId)
      credential.issuerName must be(issuer.name.value)
      credential.studentName must be(student.fullName)
      credential.title must be(request.title)
      credential.groupName must be(request.groupName)
    }
  }

  "getBy" should {
    "return the first credentials" in {
      val issuer = createIssuer("Issuer X").id
      val student = createStudent(issuer, "IOHK Student").id
      val credA = createCredential(issuer, student, "A")
      val credB = createCredential(issuer, student, "B")
      val credC = createCredential(issuer, student, "C")

      val result = repository.getBy(issuer, 2, None).value.futureValue.right.value
      result.toSet must be(Set(credA, credB))
    }

    "paginate by the last seen credential" in {
      val issuer = createIssuer("Issuer X").id
      val student = createStudent(issuer, "IOHK Student").id
      val credA = createCredential(issuer, student, "A")
      val credB = createCredential(issuer, student, "B")
      val credC = createCredential(issuer, student, "C")
      val credD = createCredential(issuer, student, "D")

      val first = repository.getBy(issuer, 2, None).value.futureValue.right.value
      val result = repository.getBy(issuer, 1, first.lastOption.map(_.id)).value.futureValue.right.value
      result.toSet must be(Set(credC))
    }
  }
}
