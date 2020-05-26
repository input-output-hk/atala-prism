package io.iohk.cvp.cmanager.repositories

import java.time.LocalDate

import io.circe.Json
import io.iohk.cvp.cmanager.models.IssuerGroup
import io.iohk.cvp.cmanager.models.requests.{CreateGenericCredential, CreateUniversityCredential}
import io.iohk.cvp.cmanager.repositories.common.CManagerRepositorySpec
import io.iohk.cvp.cmanager.repositories.common.DataPreparation._
import org.scalatest.EitherValues._
import io.circe.syntax._

class CredentialsRepositorySpec extends CManagerRepositorySpec {

  lazy val credentialsRepository = new CredentialsRepository(database)
  lazy val studentsRepository = new StudentsRepository(database)
  lazy val subjectsRepository = new IssuerSubjectsRepository(database)

  "createUniversityCredential" should {
    "create a new credential" in {
      val issuerName = "Issuer-1"
      val issuer = createIssuer(issuerName)
      val group = createIssuerGroup(issuer.id, IssuerGroup.Name("grp1"))
      val student = createStudent(issuer.id, "Student 1", group.name)
      val request = CreateUniversityCredential(
        issuedBy = issuer.id,
        studentId = student.id,
        title = "Major IN Applied Blockchain",
        enrollmentDate = LocalDate.now(),
        graduationDate = LocalDate.now().plusYears(5),
        groupName = "Computer Science"
      )

      val result = credentialsRepository.createUniversityCredential(request).value.futureValue
      val credential = result.right.value
      credential.enrollmentDate must be(request.enrollmentDate)
      credential.graduationDate must be(request.graduationDate)
      credential.issuedBy must be(request.issuedBy)
      credential.studentId must be(request.studentId)
      credential.issuerName must be(issuerName)
      credential.studentName must be(student.fullName)
      credential.title must be(request.title)
      credential.groupName must be(request.groupName)
    }
  }

  "getUniversityCredentialsBy" should {
    "return the first credentials" in {
      val issuer = createIssuer("Issuer X").id
      val group = createIssuerGroup(issuer, IssuerGroup.Name("grp1"))
      val student = createStudent(issuer, "IOHK Student", group.name).id
      val credA = createCredential(issuer, student, "A")
      val credB = createCredential(issuer, student, "B")
      val credC = createCredential(issuer, student, "C")

      val result = credentialsRepository.getUniversityCredentialsBy(issuer, 2, None).value.futureValue.right.value
      result.toSet must be(Set(credA, credB))
    }

    "paginate by the last seen credential" in {
      val issuer = createIssuer("Issuer X").id
      val group = createIssuerGroup(issuer, IssuerGroup.Name("grp1"))
      val student = createStudent(issuer, "IOHK Student", group.name).id
      val credA = createCredential(issuer, student, "A")
      val credB = createCredential(issuer, student, "B")
      val credC = createCredential(issuer, student, "C")
      val credD = createCredential(issuer, student, "D")

      val first = credentialsRepository.getUniversityCredentialsBy(issuer, 2, None).value.futureValue.right.value
      val result =
        credentialsRepository
          .getUniversityCredentialsBy(issuer, 1, first.lastOption.map(_.id))
          .value
          .futureValue
          .right
          .value
      result.toSet must be(Set(credC))
    }
  }

  // Generic versions
  "create" should {
    "create a new credential" in {
      val issuerName = "Issuer-1"
      val subjectName = "Student 1"
      val issuer = createIssuer(issuerName)
      val group = createIssuerGroup(issuer.id, IssuerGroup.Name("grp1"))
      val subject = createSubject(issuer.id, subjectName, group.name)
      val request = CreateGenericCredential(
        issuedBy = issuer.id,
        subjectId = subject.id,
        credentialData = Json.obj(
          "title" -> "Major IN Applied Blockchain".asJson,
          "enrollmentDate" -> LocalDate.now().asJson,
          "graduationDate" -> LocalDate.now().plusYears(5).asJson
        ),
        groupName = "Computer Science"
      )

      val result = credentialsRepository.create(request).value.futureValue
      val credential = result.right.value
      credential.credentialData must be(request.credentialData)
      credential.issuedBy must be(request.issuedBy)
      credential.subjectId must be(request.subjectId)
      credential.issuerName must be(issuerName)
      credential.subjectData must be(subject.data)
      credential.groupName must be(request.groupName)
    }
  }

  "getBy" should {
    "return the first credentials" in {
      val issuer = createIssuer("Issuer X").id
      val group = createIssuerGroup(issuer, IssuerGroup.Name("grp1"))
      val subject = createSubject(issuer, "IOHK Student", group.name).id
      val credA = createGenericCredential(issuer, subject, "A")
      val credB = createGenericCredential(issuer, subject, "B")
      createGenericCredential(issuer, subject, "C")

      val result = credentialsRepository.getBy(issuer, 2, None).value.futureValue.right.value
      result.toSet must be(Set(credA, credB))
    }

    "paginate by the last seen credential" in {
      val issuer = createIssuer("Issuer X").id
      val group = createIssuerGroup(issuer, IssuerGroup.Name("grp1"))
      val subject = createSubject(issuer, "IOHK Student", group.name).id
      createGenericCredential(issuer, subject, "A")
      createGenericCredential(issuer, subject, "B")
      val credC = createGenericCredential(issuer, subject, "C")
      createGenericCredential(issuer, subject, "D")

      val first = credentialsRepository.getBy(issuer, 2, None).value.futureValue.right.value
      val result =
        credentialsRepository
          .getBy(issuer, 1, first.lastOption.map(_.credentialId))
          .value
          .futureValue
          .right
          .value
      result.toSet must be(Set(credC))
    }
  }

  "getBy" should {
    "return subject's credentials" in {
      val issuerId = createIssuer("Issuer X").id
      val group = createIssuerGroup(issuerId, IssuerGroup.Name("grp1"))
      val subjectId1 = createSubject(issuerId, "IOHK Student", group.name).id
      val subjectId2 = createSubject(issuerId, "IOHK Student 2", group.name).id
      createGenericCredential(issuerId, subjectId2, "A")
      val cred1 = createGenericCredential(issuerId, subjectId1, "B")
      createGenericCredential(issuerId, subjectId2, "C")
      val cred2 = createGenericCredential(issuerId, subjectId1, "D")
      createGenericCredential(issuerId, subjectId2, "E")

      val result = credentialsRepository.getBy(issuerId, subjectId1).value.futureValue.right.value
      result must be(List(cred1, cred2))
    }

    "return empty list of credentials when not present" in {
      val issuerId = createIssuer("Issuer X").id
      val group = createIssuerGroup(issuerId, IssuerGroup.Name("grp1"))
      val subjectId = createSubject(issuerId, "IOHK Student", group.name).id

      val result = credentialsRepository.getBy(issuerId, subjectId).value.futureValue.right.value
      result must be(empty)
    }
  }
}
