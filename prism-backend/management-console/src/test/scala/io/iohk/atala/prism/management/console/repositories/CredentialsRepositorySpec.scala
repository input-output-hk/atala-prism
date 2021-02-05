package io.iohk.atala.prism.management.console.repositories

import io.circe.Json
import io.circe.syntax._
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.management.console.DataPreparation
import io.iohk.atala.prism.management.console.DataPreparation._
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.models.{Ledger, TransactionId, TransactionInfo}
import org.scalatest.OptionValues._

import java.time.LocalDate

class CredentialsRepositorySpec extends AtalaWithPostgresSpec {
  lazy val credentialsRepository = new CredentialsRepository(database)

  "create" should {
    "create a new credential" in {
      val issuerName = "Issuer-1"
      val subjectName = "Student 1"
      val issuerId = createParticipant(issuerName)
      val group = createInstitutionGroup(issuerId, InstitutionGroup.Name("grp1"))
      val subject = createContact(issuerId, subjectName, Some(group.name))
      val credentialTypeWithRequiredFields = DataPreparation.createCredentialType(issuerId, "name")
      val request = CreateGenericCredential(
        issuedBy = issuerId,
        subjectId = subject.contactId,
        credentialData = Json.obj(
          "title" -> "Major IN Applied Blockchain".asJson,
          "enrollmentDate" -> LocalDate.now().asJson,
          "graduationDate" -> LocalDate.now().plusYears(5).asJson
        ),
        credentialIssuanceContactId = None,
        credentialTypeId = Some(credentialTypeWithRequiredFields.credentialType.id)
      )

      val result = credentialsRepository.create(request).value.futureValue
      val credential = result.toOption.value
      credential.credentialData must be(request.credentialData)
      credential.issuedBy must be(request.issuedBy)
      credential.subjectId must be(request.subjectId)
      credential.issuerName must be(issuerName)
      credential.subjectData must be(subject.data)
      credential.publicationData must be(empty)
    }
  }

  "getBy" should {
    "return the credential when found" in {
      val issuerId = createParticipant("Issuer X")
      val group = createInstitutionGroup(issuerId, InstitutionGroup.Name("grp1"))
      val subjectId = createContact(issuerId, "IOHK Student 2", Some(group.name)).contactId
      val credentialTypeWithRequiredFields = DataPreparation.createCredentialType(issuerId, "name")
      val credential = createGenericCredential(
        issuerId,
        subjectId,
        "A",
        credentialTypeId = Some(credentialTypeWithRequiredFields.credentialType.id)
      )

      val returnedCredential =
        credentialsRepository.getBy(credential.credentialId).value.futureValue.toOption.value.value

      returnedCredential must be(credential)
    }

    "return a credential with publication data" in {
      val issuerId = createParticipant("Issuer X")
      val group = createInstitutionGroup(issuerId, InstitutionGroup.Name("grp1"))
      val subjectId = createContact(issuerId, "IOHK Student 2", Some(group.name)).contactId
      val credential = createGenericCredential(issuerId, subjectId, "A")
      publish(issuerId, credential.credentialId)

      credentialsRepository.getBy(credential.credentialId).value.futureValue.toOption.value.value
      succeed
    }

    "return no credential when not found" in {
      val credentialId = GenericCredential.Id.random()

      val credential = credentialsRepository.getBy(credentialId).value.futureValue.toOption.value

      credential must be(None)
    }
  }

  "getBy" should {
    "return the first credentials" in {
      val issuerId = createParticipant("Issuer X")
      val group = createInstitutionGroup(issuerId, InstitutionGroup.Name("grp1"))
      val subject = createContact(issuerId, "IOHK Student", Some(group.name)).contactId
      val credA = createGenericCredential(issuerId, subject, "A")
      val credB = createGenericCredential(issuerId, subject, "B")
      createGenericCredential(issuerId, subject, "C")

      val result = credentialsRepository.getBy(issuerId, 2, None).value.futureValue.toOption.value
      result.toSet must be(Set(credA, credB))
    }

    "return the first credentials involving a published one" in {
      val issuerId = createParticipant("Issuer X")
      val group = createInstitutionGroup(issuerId, InstitutionGroup.Name("grp1"))
      val subject = createContact(issuerId, "IOHK Student", Some(group.name)).contactId
      val credA = createGenericCredential(issuerId, subject, "A")
      val credB = createGenericCredential(issuerId, subject, "B")
      createGenericCredential(issuerId, subject, "C")
      publish(issuerId, credA.credentialId)
      credentialsRepository.getBy(credA.credentialId)

      val result = credentialsRepository.getBy(issuerId, 2, None).value.futureValue.toOption.value
      result.map(_.credentialId).toSet must be(Set(credA.credentialId, credB.credentialId))
    }

    "paginate by the last seen credential" in {
      val issuerId = createParticipant("Issuer X")
      val group = createInstitutionGroup(issuerId, InstitutionGroup.Name("grp1"))
      val subject = createContact(issuerId, "IOHK Student", Some(group.name)).contactId
      createGenericCredential(issuerId, subject, "A")
      createGenericCredential(issuerId, subject, "B")
      val credC = createGenericCredential(issuerId, subject, "C")
      createGenericCredential(issuerId, subject, "D")

      val first = credentialsRepository.getBy(issuerId, 2, None).value.futureValue.toOption.value
      val result =
        credentialsRepository
          .getBy(issuerId, 1, first.lastOption.map(_.credentialId))
          .value
          .futureValue
          .toOption
          .value
      result.toSet must be(Set(credC))
    }

    "paginate by the last seen credential including a published one" in {
      val issuerId = createParticipant("Issuer X")
      val group = createInstitutionGroup(issuerId, InstitutionGroup.Name("grp1"))
      val subject = createContact(issuerId, "IOHK Student", Some(group.name)).contactId
      createGenericCredential(issuerId, subject, "A")
      createGenericCredential(issuerId, subject, "B")
      val credC = createGenericCredential(issuerId, subject, "C")
      publish(issuerId, credC.credentialId)
      createGenericCredential(issuerId, subject, "D")

      val first = credentialsRepository.getBy(issuerId, 2, None).value.futureValue.toOption.value
      val result =
        credentialsRepository
          .getBy(issuerId, 1, first.lastOption.map(_.credentialId))
          .value
          .futureValue
          .toOption
          .value
      result.map(_.credentialId).toSet must be(Set(credC.credentialId))
    }
  }

  "getBy" should {
    "return subject's credentials" in {
      val issuerId = createParticipant("Issuer X")
      val group = createInstitutionGroup(issuerId, InstitutionGroup.Name("grp1"))
      val subjectId1 = createContact(issuerId, "IOHK Student", Some(group.name)).contactId
      val subjectId2 = createContact(issuerId, "IOHK Student 2", Some(group.name)).contactId
      createGenericCredential(issuerId, subjectId2, "A")
      val cred1 = createGenericCredential(issuerId, subjectId1, "B")
      createGenericCredential(issuerId, subjectId2, "C")
      val cred2 = createGenericCredential(issuerId, subjectId1, "D")
      createGenericCredential(issuerId, subjectId2, "E")

      val result = credentialsRepository.getBy(issuerId, subjectId1).value.futureValue.toOption.value
      result must be(List(cred1, cred2))
    }

    "return subject's credentials including a published one" in {
      val issuerId = createParticipant("Issuer X")
      val group = createInstitutionGroup(issuerId, InstitutionGroup.Name("grp1"))
      val subjectId1 = createContact(issuerId, "IOHK Student", Some(group.name)).contactId
      val subjectId2 = createContact(issuerId, "IOHK Student 2", Some(group.name)).contactId
      createGenericCredential(issuerId, subjectId2, "A")
      val cred1 = createGenericCredential(issuerId, subjectId1, "B")
      createGenericCredential(issuerId, subjectId2, "C")
      val cred2 = createGenericCredential(issuerId, subjectId1, "D")
      createGenericCredential(issuerId, subjectId2, "E")
      publish(issuerId, cred1.credentialId)

      val result = credentialsRepository.getBy(issuerId, subjectId1).value.futureValue.toOption.value
      result.map(_.credentialId) must be(List(cred1.credentialId, cred2.credentialId))
    }

    "return empty list of credentials when not present" in {
      val issuerId = createParticipant("Issuer X")
      val group = createInstitutionGroup(issuerId, InstitutionGroup.Name("grp1"))
      val subjectId = createContact(issuerId, "IOHK Student", Some(group.name)).contactId

      val result = credentialsRepository.getBy(issuerId, subjectId).value.futureValue.toOption.value
      result must be(empty)
    }
  }

  "storePublicationData" should {
    "insert credential data in db" in {
      val issuerId = createParticipant("Issuer X")
      val group = createInstitutionGroup(issuerId, InstitutionGroup.Name("grp1"))
      val subjectId = createContact(issuerId, "IOHK Student 2", Some(group.name)).contactId
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

      inserted.toOption.value must be(1)

      val credentialList =
        credentialsRepository.getBy(issuerId, subjectId).value.futureValue.toOption.value

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
      val issuerId = createParticipant("Issuer X")

      val mockOperationHash = SHA256Digest.compute("000".getBytes())
      val mockNodeCredentialId = mockOperationHash.hexValue
      val mockEncodedSignedCredential = "easdadgfkfñwlekrjfadf"
      val mockTransactionInfo = TransactionInfo(TransactionId.from(mockNodeCredentialId).value, Ledger.InMemory)

      intercept[RuntimeException](
        credentialsRepository
          .storePublicationData(
            issuerId,
            PublishCredential(
              GenericCredential.Id.random(),
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
        credentialsRepository.getBy(issuerId, 10, None).value.futureValue.toOption.value

      credentialList must be(empty)
    }

    "fail when issuer_id does not belong to the credential_id" in {
      val issuerId = createParticipant("Issuer X")
      val group = createInstitutionGroup(issuerId, InstitutionGroup.Name("grp1"))
      val subjectId = createContact(issuerId, "IOHK Student 2", Some(group.name)).contactId
      val originalCredential = createGenericCredential(issuerId, subjectId, "A")

      val mockOperationHash = SHA256Digest.compute("000".getBytes())
      val mockNodeCredentialId = mockOperationHash.hexValue
      val mockEncodedSignedCredential = "easdadgfkfñwlekrjfadf"
      val mockTransactionInfo = TransactionInfo(TransactionId.from(mockNodeCredentialId).value, Ledger.InMemory)

      intercept[RuntimeException](
        credentialsRepository
          .storePublicationData(
            ParticipantId.random(),
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
        credentialsRepository.getBy(issuerId, subjectId).value.futureValue.toOption.value

      credentialList.length must be(1)

      val updatedCredential = credentialList.headOption.value

      updatedCredential must be(originalCredential)
      updatedCredential.publicationData must be(empty)
    }
  }

  "markAsShared" should {
    "work" in {
      val issuerId = createParticipant("Issuer X")
      val subjectId = createContact(issuerId, "IOHK Student", None).contactId
      val credential = createGenericCredential(issuerId, subjectId, "A")
      publish(issuerId, credential.credentialId)
      credentialsRepository.markAsShared(issuerId, credential.credentialId).value.futureValue.toOption.value

      val result = credentialsRepository.getBy(credential.credentialId).value.futureValue.toOption.value.value
      result.sharedAt mustNot be(empty)
    }

    "set a new date even if the credential was shared before" in {
      val issuerId = createParticipant("Issuer X")
      val subjectId = createContact(issuerId, "IOHK Student", None).contactId
      val credential = createGenericCredential(issuerId, subjectId, "A")
      publish(issuerId, credential.credentialId)
      credentialsRepository.markAsShared(issuerId, credential.credentialId).value.futureValue.toOption.value
      val result1 = credentialsRepository
        .getBy(credential.credentialId)
        .value
        .futureValue
        .toOption
        .value
        .value
        .sharedAt
        .value

      credentialsRepository.markAsShared(issuerId, credential.credentialId).value.futureValue.toOption.value
      val result2 = credentialsRepository
        .getBy(credential.credentialId)
        .value
        .futureValue
        .toOption
        .value
        .value
        .sharedAt
        .value
      (result2.compareTo(result1) > 0) must be(true)
    }

    "fail when the credential hasn't been published" in {
      val issuerId = createParticipant("Issuer X")
      val subjectId = createContact(issuerId, "IOHK Student", None).contactId
      val credential = createGenericCredential(issuerId, subjectId, "A")
      assertThrows[Exception] {
        credentialsRepository.markAsShared(issuerId, credential.credentialId).value.futureValue.toOption.value
      }
    }

    "fail when the credential doesn't belong to the given issuer" in {
      val issuerId = createParticipant("Issuer X")
      val issuerId2 = createParticipant("Issuer Y")
      val subjectId = createContact(issuerId, "IOHK Student", None).contactId
      val credential = createGenericCredential(issuerId, subjectId, "A")
      publish(issuerId, credential.credentialId)
      assertThrows[Exception] {
        credentialsRepository.markAsShared(issuerId2, credential.credentialId).value.futureValue.toOption.value
      }
    }
  }

  private def publish(issuerId: ParticipantId, id: GenericCredential.Id): Unit = {
    val _ = credentialsRepository
      .storePublicationData(
        issuerId,
        PublishCredential(
          id,
          SHA256Digest.compute("test".getBytes),
          "mockNodeCredentialId",
          "mockEncodedSignedCredential",
          TransactionInfo(
            TransactionId.from("3d488d9381b09954b5a9606b365ab0aaeca6aa750bdba79436e416ad6702226a").value,
            Ledger.InMemory,
            None
          )
        )
      )
      .value
      .futureValue
  }
}
