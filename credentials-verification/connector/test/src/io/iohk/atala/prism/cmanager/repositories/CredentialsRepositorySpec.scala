package io.iohk.atala.prism.cmanager.repositories

import java.time.LocalDate
import java.util.UUID

import io.circe.Json
import io.iohk.atala.prism.cmanager.models.{GenericCredential, Student}
import io.iohk.atala.prism.cmanager.models.requests.{
  CreateGenericCredential,
  CreateUniversityCredential,
  PublishCredential
}
import io.iohk.atala.prism.cmanager.repositories.common.CManagerRepositorySpec
import io.iohk.atala.prism.cmanager.repositories.common.DataPreparation._
import org.scalatest.EitherValues._
import org.scalatest.OptionValues._
import io.circe.syntax._
import io.iohk.atala.prism.console.models.{Institution, IssuerGroup}
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.models.{Ledger, TransactionId, TransactionInfo}

class CredentialsRepositorySpec extends CManagerRepositorySpec {

  lazy val credentialsRepository = new CredentialsRepository(database)

  "createUniversityCredential" should {
    "create a new credential" in {
      val issuerName = "Issuer-1"
      val issuerId = createIssuer(issuerName)
      val group = createIssuerGroup(issuerId, IssuerGroup.Name("grp1"))
      val student = createStudent(issuerId, "Student 1", group.name)
      val request = CreateUniversityCredential(
        issuedBy = issuerId,
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
      val issuerId = createIssuer("Issuer X")
      val group = createIssuerGroup(issuerId, IssuerGroup.Name("grp1"))
      val student = createStudent(issuerId, "IOHK Student", group.name).id
      val credA = createCredential(issuerId, student, "A")
      val credB = createCredential(issuerId, student, "B")
      createCredential(issuerId, student, "C")

      val result = credentialsRepository.getUniversityCredentialsBy(issuerId, 2, None).value.futureValue.right.value
      result.toSet must be(Set(credA, credB))
    }

    "paginate by the last seen credential" in {
      val issuerId = createIssuer("Issuer X")
      val group = createIssuerGroup(issuerId, IssuerGroup.Name("grp1"))
      val student = createStudent(issuerId, "IOHK Student", group.name).id
      createCredential(issuerId, student, "A")
      createCredential(issuerId, student, "B")
      val credC = createCredential(issuerId, student, "C")
      createCredential(issuerId, student, "D")

      val first = credentialsRepository.getUniversityCredentialsBy(issuerId, 2, None).value.futureValue.right.value
      val result =
        credentialsRepository
          .getUniversityCredentialsBy(issuerId, 1, first.lastOption.map(_.id))
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
      val issuerId = createIssuer(issuerName)
      val group = createIssuerGroup(issuerId, IssuerGroup.Name("grp1"))
      val subject = createContact(issuerId, subjectName, group.name)
      val request = CreateGenericCredential(
        issuedBy = issuerId,
        subjectId = subject.contactId,
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
      credential.publicationData must be(empty)
      credential.connectionStatus must be(Student.ConnectionStatus.InvitationMissing)
    }
  }

  "getBy" should {
    "return the credential when found" in {
      val issuerId = createIssuer("Issuer X")
      val group = createIssuerGroup(issuerId, IssuerGroup.Name("grp1"))
      val subjectId = createContact(issuerId, "IOHK Student 2", group.name).contactId
      val credential = createGenericCredential(issuerId, subjectId, "A")

      val returnedCredential = credentialsRepository.getBy(credential.credentialId).value.futureValue.right.value.value

      returnedCredential must be(credential)
    }

    "return no credential when not found" in {
      val credentialId = GenericCredential.Id(UUID.randomUUID())

      val credential = credentialsRepository.getBy(credentialId).value.futureValue.right.value

      credential must be(None)
    }
  }

  "getBy" should {
    "return the first credentials" in {
      val issuerId = createIssuer("Issuer X")
      val group = createIssuerGroup(issuerId, IssuerGroup.Name("grp1"))
      val subject = createContact(issuerId, "IOHK Student", group.name).contactId
      val credA = createGenericCredential(issuerId, subject, "A")
      val credB = createGenericCredential(issuerId, subject, "B")
      createGenericCredential(issuerId, subject, "C")

      val result = credentialsRepository.getBy(issuerId, 2, None).value.futureValue.right.value
      result.toSet must be(Set(credA, credB))
    }

    "paginate by the last seen credential" in {
      val issuerId = createIssuer("Issuer X")
      val group = createIssuerGroup(issuerId, IssuerGroup.Name("grp1"))
      val subject = createContact(issuerId, "IOHK Student", group.name).contactId
      createGenericCredential(issuerId, subject, "A")
      createGenericCredential(issuerId, subject, "B")
      val credC = createGenericCredential(issuerId, subject, "C")
      createGenericCredential(issuerId, subject, "D")

      val first = credentialsRepository.getBy(issuerId, 2, None).value.futureValue.right.value
      val result =
        credentialsRepository
          .getBy(issuerId, 1, first.lastOption.map(_.credentialId))
          .value
          .futureValue
          .right
          .value
      result.toSet must be(Set(credC))
    }
  }

  "getBy" should {
    "return subject's credentials" in {
      val issuerId = createIssuer("Issuer X")
      val group = createIssuerGroup(issuerId, IssuerGroup.Name("grp1"))
      val subjectId1 = createContact(issuerId, "IOHK Student", group.name).contactId
      val subjectId2 = createContact(issuerId, "IOHK Student 2", group.name).contactId
      createGenericCredential(issuerId, subjectId2, "A")
      val cred1 = createGenericCredential(issuerId, subjectId1, "B")
      createGenericCredential(issuerId, subjectId2, "C")
      val cred2 = createGenericCredential(issuerId, subjectId1, "D")
      createGenericCredential(issuerId, subjectId2, "E")

      val result = credentialsRepository.getBy(issuerId, subjectId1).value.futureValue.right.value
      result must be(List(cred1, cred2))
    }

    "return empty list of credentials when not present" in {
      val issuerId = createIssuer("Issuer X")
      val group = createIssuerGroup(issuerId, IssuerGroup.Name("grp1"))
      val subjectId = createContact(issuerId, "IOHK Student", group.name).contactId

      val result = credentialsRepository.getBy(issuerId, subjectId).value.futureValue.right.value
      result must be(empty)
    }
  }

  "storePublicationData" should {
    "insert credential data in db" in {
      val issuerId = createIssuer("Issuer X")
      val group = createIssuerGroup(issuerId, IssuerGroup.Name("grp1"))
      val subjectId = createContact(issuerId, "IOHK Student 2", group.name).contactId
      val originalCredential = createGenericCredential(issuerId, subjectId, "A")

      val mockOperationHash = SHA256Digest.compute("000".getBytes())
      val mockNodeCredentialId = mockOperationHash.hexValue
      val mockEncodedSignedCredential = "easdadgfkfñwlekrjfadf"
      val mockTransactionInfo = TransactionInfo(TransactionId.from(mockNodeCredentialId).value, Ledger.InMemory)

      val inserted = credentialsRepository
        .storePublicationData(
          issuerId,
          PublishCredential(
            originalCredential.credentialId,
            mockOperationHash,
            mockNodeCredentialId,
            mockEncodedSignedCredential,
            mockTransactionInfo
          )
        )
        .value
        .futureValue

      inserted.right.value must be(1)

      val credentialList =
        credentialsRepository.getBy(issuerId, subjectId).value.futureValue.right.value

      credentialList.length must be(1)

      val updatedCredential = credentialList.headOption.value
      val publicationData = updatedCredential.publicationData.value

      publicationData.nodeCredentialId must be(mockNodeCredentialId)
      publicationData.issuanceOperationHash must be(mockOperationHash)
      publicationData.encodedSignedCredential must be(mockEncodedSignedCredential)
      publicationData.transactionId must be(mockTransactionInfo.transactionId)
      publicationData.ledger must be(mockTransactionInfo.ledger)
      // the rest should remain unchanged
      updatedCredential.copy(publicationData = None) must be(originalCredential)
    }

    "fail when credential_id is not registered" in {
      val issuerId = createIssuer("Issuer X")

      val mockOperationHash = SHA256Digest.compute("000".getBytes())
      val mockNodeCredentialId = mockOperationHash.hexValue
      val mockEncodedSignedCredential = "easdadgfkfñwlekrjfadf"
      val mockTransactionInfo = TransactionInfo(TransactionId.from(mockNodeCredentialId).value, Ledger.InMemory)

      intercept[RuntimeException](
        credentialsRepository
          .storePublicationData(
            issuerId,
            PublishCredential(
              GenericCredential.Id(UUID.randomUUID()),
              mockOperationHash,
              mockNodeCredentialId,
              mockEncodedSignedCredential,
              mockTransactionInfo
            )
          )
          .value
          .futureValue
      )

      val credentialList =
        credentialsRepository.getBy(issuerId, 10, None).value.futureValue.right.value

      credentialList must be(empty)
    }

    "fail when issuer_id does not belong to the credential_id" in {
      val issuerId = createIssuer("Issuer X")
      val group = createIssuerGroup(issuerId, IssuerGroup.Name("grp1"))
      val subjectId = createContact(issuerId, "IOHK Student 2", group.name).contactId
      val originalCredential = createGenericCredential(issuerId, subjectId, "A")

      val mockOperationHash = SHA256Digest.compute("000".getBytes())
      val mockNodeCredentialId = mockOperationHash.hexValue
      val mockEncodedSignedCredential = "easdadgfkfñwlekrjfadf"
      val mockTransactionInfo = TransactionInfo(TransactionId.from(mockNodeCredentialId).value, Ledger.InMemory)

      intercept[RuntimeException](
        credentialsRepository
          .storePublicationData(
            Institution.Id(UUID.randomUUID()),
            PublishCredential(
              originalCredential.credentialId,
              mockOperationHash,
              mockNodeCredentialId,
              mockEncodedSignedCredential,
              mockTransactionInfo
            )
          )
          .value
          .futureValue
      )

      val credentialList =
        credentialsRepository.getBy(issuerId, subjectId).value.futureValue.right.value

      credentialList.length must be(1)

      val updatedCredential = credentialList.headOption.value

      updatedCredential must be(originalCredential)
      updatedCredential.publicationData must be(empty)
    }
  }
}
