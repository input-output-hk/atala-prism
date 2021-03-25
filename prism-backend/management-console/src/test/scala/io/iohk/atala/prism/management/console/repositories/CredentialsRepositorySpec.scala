package io.iohk.atala.prism.management.console.repositories

import cats.data.NonEmptyList
import cats.effect.IO
import doobie.util.transactor
import doobie.implicits._
import io.circe.Json
import io.circe.syntax._
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.MerkleTree.MerkleInclusionProof
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.management.console.DataPreparation
import io.iohk.atala.prism.management.console.DataPreparation._
import io.iohk.atala.prism.management.console.errors.{
  CredentialDataValidationFailed,
  PublishedCredentialsNotExist,
  PublishedCredentialsNotRevoked
}
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.models.{Ledger, TransactionId, TransactionInfo}
import org.scalatest.OptionValues._

class CredentialsRepositorySpec extends AtalaWithPostgresSpec {
  import CredentialsRepositorySpec.publish

  lazy val credentialsRepository = new CredentialsRepository(database)

  "create" should {
    "create a new credential" in {
      val issuerName = "Issuer-1"
      val subjectName = "Student 1"
      val issuerId = createParticipant(issuerName)
      val group = createInstitutionGroup(issuerId, InstitutionGroup.Name("grp1"))
      val contact = createContact(issuerId, subjectName, Some(group.name))
      val credentialTypeWithRequiredFields = DataPreparation.createCredentialType(issuerId, "name")
      val request = CreateGenericCredential(
        credentialData = Json.obj(
          "title" -> "Major IN Applied Blockchain".asJson,
          "enrollmentDate" -> "01/10/2010".asJson,
          "graduationDate" -> "01/07/2015".asJson
        ),
        credentialIssuanceContactId = None,
        credentialTypeId = credentialTypeWithRequiredFields.credentialType.id,
        contactId = Some(contact.contactId),
        externalId = None
      )

      val result = credentialsRepository.create(issuerId, request).value.futureValue
      val credential = result.toOption.value
      credential.credentialData must be(request.credentialData)
      credential.issuedBy must be(issuerId)
      credential.subjectId must be(request.contactId.value)
      credential.issuerName must be(issuerName)
      credential.subjectData must be(contact.data)
      credential.publicationData must be(empty)
    }

    "fail to create a new credential when referenced credential type can't be rendered with specified credential data" in {
      val issuerName = "Issuer-1"
      val subjectName = "Student 1"
      val issuerId = createParticipant(issuerName)
      val contact = createContact(issuerId, subjectName, None)
      val credentialTypeWithRequiredFields = DataPreparation.createCredentialType(issuerId, "name")
      val request = CreateGenericCredential(
        credentialData = Json.obj(
          "title" -> "Major IN Applied Blockchain".asJson,
          "enrollmentDate" -> "01/10/2010".asJson
        ),
        credentialIssuanceContactId = None,
        credentialTypeId = credentialTypeWithRequiredFields.credentialType.id,
        contactId = Some(contact.contactId),
        externalId = None
      )

      val result = credentialsRepository.create(issuerId, request).value.futureValue
      result mustBe a[Left[CredentialDataValidationFailed, _]]
    }
  }

  "getBy" should {
    "return the credential when found" in {
      val issuerId = createParticipant("Issuer X")
      val group = createInstitutionGroup(issuerId, InstitutionGroup.Name("grp1"))
      val subjectId = createContact(issuerId, "IOHK Student 2", Some(group.name)).contactId
      val credential = createGenericCredential(
        issuerId,
        subjectId,
        "A"
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
      val mockBatchId = CredentialBatchId.fromDigest(mockOperationHash)
      val mockTransactionId =
        TransactionId.from("1423856bc91c49e928f6f30f4e8d665d53eb4ab6028bd0ac971809d514c92db1").value
      val mockLedger = Ledger.InMemory
      val mockTransactionInfo = TransactionInfo(mockTransactionId, mockLedger)

      /// we first publish the batch
      DataPreparation.publishBatch(mockBatchId, mockOperationHash, mockTransactionInfo)

      val mockEncodedSignedCredential = "easdadgfkfñwlekrjfadf"
      val mockMerkleProof = MerkleInclusionProof(mockOperationHash, 1, List(mockOperationHash))

      val inserted = credentialsRepository
        .storePublicationData(
          issuerId,
          PublishCredential(
            originalCredential.credentialId,
            mockBatchId,
            mockEncodedSignedCredential,
            mockMerkleProof
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

      publicationData.credentialBatchId must be(mockBatchId)
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
      val mockBatchId = CredentialBatchId.fromDigest(mockOperationHash)
      val mockTransactionId =
        TransactionId.from("1423856bc91c49e928f6f30f4e8d665d53eb4ab6028bd0ac971809d514c92db1").value
      val mockLedger = Ledger.InMemory
      val mockTransactionInfo = TransactionInfo(mockTransactionId, mockLedger)

      /// we first publish the batch
      DataPreparation.publishBatch(mockBatchId, mockOperationHash, mockTransactionInfo)

      val mockEncodedSignedCredential = "easdadgfkfñwlekrjfadf"
      val mockMerkleProof = MerkleInclusionProof(mockOperationHash, 1, List(mockOperationHash))

      intercept[RuntimeException](
        credentialsRepository
          .storePublicationData(
            issuerId,
            PublishCredential(
              GenericCredential.Id.random(),
              mockBatchId,
              mockEncodedSignedCredential,
              mockMerkleProof
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
      val mockBatchId = CredentialBatchId.fromDigest(mockOperationHash)
      val mockTransactionId =
        TransactionId.from("1423856bc91c49e928f6f30f4e8d665d53eb4ab6028bd0ac971809d514c92db1").value
      val mockLedger = Ledger.InMemory
      val mockTransactionInfo = TransactionInfo(mockTransactionId, mockLedger)

      /// we first publish the batch
      DataPreparation.publishBatch(mockBatchId, mockOperationHash, mockTransactionInfo)

      val mockEncodedSignedCredential = "easdadgfkfñwlekrjfadf"
      val mockMerkleProof = MerkleInclusionProof(mockOperationHash, 1, List(mockOperationHash))

      intercept[RuntimeException](
        credentialsRepository
          .storePublicationData(
            ParticipantId.random(),
            PublishCredential(
              originalCredential.credentialId,
              mockBatchId,
              mockEncodedSignedCredential,
              mockMerkleProof
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

    "fail when the batch id does not exist" in {
      val issuerId = createParticipant("Issuer X")
      val group = createInstitutionGroup(issuerId, InstitutionGroup.Name("grp1"))
      val subjectId = createContact(issuerId, "IOHK Student 2", Some(group.name)).contactId
      val originalCredential = createGenericCredential(issuerId, subjectId, "A")

      val mockHash = SHA256Digest.compute("000".getBytes())
      val mockBatchId = CredentialBatchId.fromDigest(mockHash)
      val mockEncodedSignedCredential = "easdadgfkfñwlekrjfadf"
      val mockMerkleProof = MerkleInclusionProof(mockHash, 1, List(mockHash))

      intercept[RuntimeException](
        credentialsRepository
          .storePublicationData(
            issuerId,
            PublishCredential(
              originalCredential.credentialId,
              mockBatchId,
              mockEncodedSignedCredential,
              mockMerkleProof
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

      val credential1 = createGenericCredential(issuerId, subjectId, "A")
      val credential2 = createGenericCredential(issuerId, subjectId, "B")

      publishCredential(issuerId, credential1)
      publishCredential(issuerId, credential2)

      credentialsRepository
        .markAsShared(issuerId, NonEmptyList.of(credential1.credentialId, credential2.credentialId))
        .value
        .futureValue
        .toOption
        .value

      val result1 = credentialsRepository.getBy(credential1.credentialId).value.futureValue.toOption.value.value
      result1.sharedAt mustNot be(empty)

      val result2 = credentialsRepository.getBy(credential2.credentialId).value.futureValue.toOption.value.value
      result2.sharedAt mustNot be(empty)
    }

    "set a new date even if the credential was shared before" in {
      val issuerId = createParticipant("Issuer X")
      val subjectId = createContact(issuerId, "IOHK Student", None).contactId
      val credential = createGenericCredential(issuerId, subjectId, "A")

      publish(issuerId, credential.credentialId)
      credentialsRepository
        .markAsShared(issuerId, NonEmptyList.of(credential.credentialId))
        .value
        .futureValue
        .toOption
        .value

      val result1 = credentialsRepository
        .getBy(credential.credentialId)
        .value
        .futureValue
        .toOption
        .value
        .value
        .sharedAt
        .value

      credentialsRepository
        .markAsShared(issuerId, NonEmptyList.of(credential.credentialId))
        .value
        .futureValue
        .toOption
        .value
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
        credentialsRepository
          .markAsShared(issuerId, NonEmptyList.of(credential.credentialId))
          .value
          .futureValue
          .toOption
          .value
      }
    }

    "fail when the credential doesn't belong to the given issuer" in {
      val issuerId = createParticipant("Issuer X")
      val issuerId2 = createParticipant("Issuer Y")
      val subjectId = createContact(issuerId, "IOHK Student", None).contactId
      val credential = createGenericCredential(issuerId, subjectId, "A")
      publish(issuerId, credential.credentialId)
      assertThrows[Exception] {
        credentialsRepository
          .markAsShared(issuerId2, NonEmptyList.of(credential.credentialId))
          .value
          .futureValue
          .toOption
          .value
      }
    }
  }

  "verifyPublishedCredentialsExist" should {
    "work" in {
      val issuerId = createParticipant("Issuer X")
      val subjectId = createContact(issuerId, "IOHK Student", None).contactId

      val credential1 = createGenericCredential(issuerId, subjectId, "A")
      val credential2 = createGenericCredential(issuerId, subjectId, "B")

      publishCredential(issuerId, credential1)
      publishCredential(issuerId, credential2)

      val result = credentialsRepository
        .verifyPublishedCredentialsExist(issuerId, NonEmptyList.of(credential1.credentialId, credential2.credentialId))
        .value
        .futureValue

      result mustBe a[Right[_, _]]
    }

    "return error when one of the credential is not found or published" in {
      val issuerId = createParticipant("Issuer X")
      val subjectId = createContact(issuerId, "IOHK Student", None).contactId

      val credential1 = createGenericCredential(issuerId, subjectId, "A")
      val credential2 = createGenericCredential(issuerId, subjectId, "B")

      publishCredential(issuerId, credential1)

      val result = credentialsRepository
        .verifyPublishedCredentialsExist(issuerId, NonEmptyList.of(credential1.credentialId, credential2.credentialId))
        .value
        .futureValue

      result mustBe Left(PublishedCredentialsNotExist(List(credential2.credentialId)))
    }

  }

  "storeBatchData" should {
    "insert the expected data" in {
      val mockHash = SHA256Digest.compute("random".getBytes())
      val mockBatchId = CredentialBatchId.fromDigest(mockHash)
      val mockLedger = Ledger.InMemory
      val mockTransactionId =
        TransactionId.from("1423856bc91c49e928f6f30f4e8d665d53eb4ab6028bd0ac971809d514c92db1").value

      credentialsRepository
        .storeBatchData(
          mockBatchId,
          mockHash,
          TransactionInfo(mockTransactionId, mockLedger, None)
        )
        .value
        .futureValue

      val (transactionId, ledger, hash) = DataPreparation.getBatchData(mockBatchId).value

      transactionId mustBe mockTransactionId
      ledger mustBe mockLedger
      hash mustBe mockHash
    }
  }

  "deleteCredentials" should {
    "delete draft and published, revoked credentials" in {
      val issuerId = createParticipant("Issuer X")
      val subjectId = createContact(issuerId, "IOHK Student", None).contactId

      val credential1 = createGenericCredential(issuerId, subjectId, "A")
      val credential2 = createGenericCredential(issuerId, subjectId, "B")

      publishCredential(issuerId, credential1)
      markAsRevoked(credential1.credentialId)

      val result = credentialsRepository
        .deleteCredentials(issuerId, NonEmptyList.of(credential1.credentialId, credential2.credentialId))
        .value
        .futureValue

      result mustBe a[Right[_, _]]
      credentialsRepository.getBy(credential1.credentialId).toFuture.futureValue mustBe None
      credentialsRepository.getBy(credential2.credentialId).toFuture.futureValue mustBe None
    }

    "do not delete credentials when one of them is published and not revoked" in {
      val issuerId = createParticipant("Issuer X")
      val subjectId = createContact(issuerId, "IOHK Student", None).contactId

      val credential1 = createGenericCredential(issuerId, subjectId, "A")
      val credential2 = createGenericCredential(issuerId, subjectId, "B")

      publishCredential(issuerId, credential1)

      val result = credentialsRepository
        .deleteCredentials(issuerId, NonEmptyList.of(credential1.credentialId, credential2.credentialId))
        .value
        .futureValue

      result mustBe Left(PublishedCredentialsNotRevoked(List(credential1.credentialId)))
      credentialsRepository.getBy(credential1.credentialId).toFuture.futureValue mustBe a[Some[_]]
      credentialsRepository.getBy(credential2.credentialId).toFuture.futureValue mustBe a[Some[_]]
    }

    def markAsRevoked(credentialId: GenericCredential.Id): Unit = {
      val transactionId = 1.to(64).map(_ => "a").mkString("")
      (sql"""
            |UPDATE published_credentials
            |SET revoked_on_transaction_id = decode(${transactionId}, 'hex')
            |WHERE credential_id = ${credentialId.uuid.toString}::uuid
       """.stripMargin.update.run.map(_ => ())).transact(database).unsafeRunSync()
    }
  }

  "storeRevocationData" should {
    "store the related transaction id" in {
      val institutionId = createParticipant("Issuer X")
      val contactId = createContact(institutionId, "IOHK Student 2", None).contactId
      val credential = createGenericCredential(institutionId, contactId, "A")

      val mockOperationHash = SHA256Digest.compute("000".getBytes())
      val mockBatchId = CredentialBatchId.fromDigest(mockOperationHash)
      val mockTransactionId = TransactionId
        .from("1423856bc91c49e928f6f30f4e8d665d53eb4ab6028bd0ac971809d514c92db1")
        .value
      val mockLedger = Ledger.InMemory
      val mockTransactionInfo = TransactionInfo(mockTransactionId, mockLedger)

      // we first publish the batch
      publishBatch(mockBatchId, mockOperationHash, mockTransactionInfo)

      val mockEncodedSignedCredential = "easdadgfkfñwlekrjfadf"
      val mockMerkleProof = MerkleInclusionProof(mockOperationHash, 1, List(mockOperationHash))

      credentialsRepository
        .storePublicationData(
          institutionId,
          PublishCredential(
            credential.credentialId,
            mockBatchId,
            mockEncodedSignedCredential,
            mockMerkleProof
          )
        )
        .value
        .futureValue
        .toOption
        .value must be(1)

      val revocationTransactionId = TransactionId
        .from("98765432c91c49e928f6f30f4e8d665d53eb4ab6028bd0ac971809d514c92db1")
        .value
      credentialsRepository
        .storeRevocationData(institutionId, credential.credentialId, revocationTransactionId)
        .value
        .futureValue

      val result = credentialsRepository
        .getBy(institutionId, contactId)
        .value
        .futureValue
        .toOption
        .value
        .headOption
        .value

      result.revokedOnTransactionId.value must be(revocationTransactionId)
    }

    "fail when the credential doesn't exist" in {
      val institutionId = createParticipant("Issuer X")
      val contactId = createContact(institutionId, "IOHK Student 2", None).contactId
      createGenericCredential(institutionId, contactId, "A")

      val revocationTransactionId = TransactionId
        .from("98765432c91c49e928f6f30f4e8d665d53eb4ab6028bd0ac971809d514c92db1")
        .value

      intercept[RuntimeException] {
        credentialsRepository
          .storeRevocationData(institutionId, GenericCredential.Id.random(), revocationTransactionId)
          .value
          .futureValue
      }
    }

    "fail when the credential does not belong to the institution" in {
      val institutionId = createParticipant("Issuer X")
      val contactId = createContact(institutionId, "IOHK Student 2", None).contactId
      val credential = createGenericCredential(institutionId, contactId, "A")

      val mockOperationHash = SHA256Digest.compute("000".getBytes())
      val mockBatchId = CredentialBatchId.fromDigest(mockOperationHash)
      val mockTransactionId = TransactionId
        .from("1423856bc91c49e928f6f30f4e8d665d53eb4ab6028bd0ac971809d514c92db1")
        .value
      val mockLedger = Ledger.InMemory
      val mockTransactionInfo = TransactionInfo(mockTransactionId, mockLedger)

      // we first publish the batch
      publishBatch(mockBatchId, mockOperationHash, mockTransactionInfo)

      val mockEncodedSignedCredential = "easdadgfkfñwlekrjfadf"
      val mockMerkleProof = MerkleInclusionProof(mockOperationHash, 1, List(mockOperationHash))

      credentialsRepository
        .storePublicationData(
          institutionId,
          PublishCredential(
            credential.credentialId,
            mockBatchId,
            mockEncodedSignedCredential,
            mockMerkleProof
          )
        )
        .value
        .futureValue
        .toOption
        .value must be(1)

      intercept[RuntimeException] {
        val revocationTransactionId = TransactionId
          .from("98765432c91c49e928f6f30f4e8d665d53eb4ab6028bd0ac971809d514c92db1")
          .value
        credentialsRepository
          .storeRevocationData(createParticipant("Issuer Y"), credential.credentialId, revocationTransactionId)
          .value
          .futureValue
      }
    }
  }
}

object CredentialsRepositorySpec {
  private val aHash = SHA256Digest.compute("random string".getBytes())
  private val aBatchId = CredentialBatchId.fromDigest(aHash)
  private val aTxInfo = TransactionInfo(
    TransactionId.from("3d488d9381b09954b5a9606b365ab0aaeca6aa750bdba79436e416ad6702226a").value,
    Ledger.InMemory,
    None
  )
  private val aProof = MerkleInclusionProof(aHash, 1, List(aHash))
  private val anEncodedCred = "encodedSignedCredenital"

  def publish(
      issuerId: ParticipantId,
      consoleId: GenericCredential.Id
  )(implicit database: transactor.Transactor[IO]): Unit = {
    DataPreparation.publishBatch(aBatchId, aHash, aTxInfo)
    DataPreparation.publishCredential(issuerId, aBatchId, consoleId, anEncodedCred, aProof)
  }
}
