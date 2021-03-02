package io.iohk.atala.prism.console.repositories

import io.circe.Json
import io.circe.syntax._
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.connector.model.ConnectionStatus
import io.iohk.atala.prism.console.DataPreparation._
import io.iohk.atala.prism.console.models.{
  CreateGenericCredential,
  CredentialPublicationData,
  GenericCredential,
  Institution,
  IssuerGroup,
  PublicationData,
  StoreBatchData
}
import org.scalatest.OptionValues._
import java.time.{Instant, LocalDate}

import io.iohk.atala.prism.console.DataPreparation
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.MerkleTree.MerkleInclusionProof
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.models.{Ledger, TransactionId, TransactionInfo}

class CredentialsRepositorySpec extends AtalaWithPostgresSpec {
  lazy implicit val credentialsRepository: CredentialsRepository = new CredentialsRepository(database)

  private val aHash = SHA256Digest.compute("random string".getBytes())
  private val aBatchId = CredentialBatchId.fromDigest(aHash)
  private val aTxInfo = TransactionInfo(
    TransactionId.from("3d488d9381b09954b5a9606b365ab0aaeca6aa750bdba79436e416ad6702226a").value,
    Ledger.InMemory,
    None
  )
  private val aProof = MerkleInclusionProof(aHash, 1, List(aHash))
  private val anEncodedCred = "encodedSignedCredenital"

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
      val credential = result.toOption.value
      credential.credentialData must be(request.credentialData)
      credential.issuedBy must be(request.issuedBy)
      credential.subjectId must be(request.subjectId)
      credential.issuerName must be(issuerName)
      credential.subjectData must be(subject.data)
      credential.groupName must be(request.groupName)
      credential.publicationData must be(empty)
      credential.connectionStatus must be(ConnectionStatus.InvitationMissing)
    }
  }

  "getBy" should {
    "return the credential when found" in {
      val issuerId = createIssuer("Issuer X")
      val group = createIssuerGroup(issuerId, IssuerGroup.Name("grp1"))
      val subjectId = createContact(issuerId, "IOHK Student 2", group.name).contactId
      val credential = createGenericCredential(issuerId, subjectId, "A")

      val returnedCredential =
        credentialsRepository.getBy(credential.credentialId).value.futureValue.toOption.value.value

      returnedCredential must be(credential)
    }

    "return a credential with publication data" in {
      val issuerId = createIssuer("Issuer X")
      val group = createIssuerGroup(issuerId, IssuerGroup.Name("grp1"))
      val subjectId = createContact(issuerId, "IOHK Student 2", group.name).contactId
      val credential = createGenericCredential(issuerId, subjectId, "A")
      publish(issuerId, credential.credentialId)

      val aTimestamp = Instant.now()
      val publicationData = credentialsRepository
        .getBy(credential.credentialId)
        .value
        .futureValue
        .toOption
        .value
        .value
        .publicationData
        .value

      val expectedPublicationData = PublicationData(
        aBatchId,
        aHash,
        anEncodedCred,
        aProof,
        aTimestamp,
        aTxInfo.transactionId,
        aTxInfo.ledger
      )

      publicationData.copy(storedAt = aTimestamp) must be(expectedPublicationData)
    }

    "return no credential when not found" in {
      val credentialId = GenericCredential.Id.random()

      val credential = credentialsRepository.getBy(credentialId).value.futureValue.toOption.value

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

      val result = credentialsRepository.getBy(issuerId, 2, None).value.futureValue.toOption.value
      result.toSet must be(Set(credA, credB))
    }

    "return the first credentials involving a published one" in {
      val issuerId = createIssuer("Issuer X")
      val group = createIssuerGroup(issuerId, IssuerGroup.Name("grp1"))
      val subject = createContact(issuerId, "IOHK Student", group.name).contactId
      val credA = createGenericCredential(issuerId, subject, "A")
      val credB = createGenericCredential(issuerId, subject, "B")
      createGenericCredential(issuerId, subject, "C")
      publish(issuerId, credA.credentialId)
      credentialsRepository.getBy(credA.credentialId)

      val result = credentialsRepository.getBy(issuerId, 2, None).value.futureValue.toOption.value
      result.map(_.credentialId).toSet must be(Set(credA.credentialId, credB.credentialId))
    }

    "paginate by the last seen credential" in {
      val issuerId = createIssuer("Issuer X")
      val group = createIssuerGroup(issuerId, IssuerGroup.Name("grp1"))
      val subject = createContact(issuerId, "IOHK Student", group.name).contactId
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
      val issuerId = createIssuer("Issuer X")
      val group = createIssuerGroup(issuerId, IssuerGroup.Name("grp1"))
      val subject = createContact(issuerId, "IOHK Student", group.name).contactId
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
      val issuerId = createIssuer("Issuer X")
      val group = createIssuerGroup(issuerId, IssuerGroup.Name("grp1"))
      val subjectId1 = createContact(issuerId, "IOHK Student", group.name).contactId
      val subjectId2 = createContact(issuerId, "IOHK Student 2", group.name).contactId
      createGenericCredential(issuerId, subjectId2, "A")
      val cred1 = createGenericCredential(issuerId, subjectId1, "B")
      createGenericCredential(issuerId, subjectId2, "C")
      val cred2 = createGenericCredential(issuerId, subjectId1, "D")
      createGenericCredential(issuerId, subjectId2, "E")

      val result = credentialsRepository.getBy(issuerId, subjectId1).value.futureValue.toOption.value
      result must be(List(cred1, cred2))
    }

    "return subject's credentials including a published one" in {
      val issuerId = createIssuer("Issuer X")
      val group = createIssuerGroup(issuerId, IssuerGroup.Name("grp1"))
      val subjectId1 = createContact(issuerId, "IOHK Student", group.name).contactId
      val subjectId2 = createContact(issuerId, "IOHK Student 2", group.name).contactId
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
      val issuerId = createIssuer("Issuer X")
      val group = createIssuerGroup(issuerId, IssuerGroup.Name("grp1"))
      val subjectId = createContact(issuerId, "IOHK Student", group.name).contactId

      val result = credentialsRepository.getBy(issuerId, subjectId).value.futureValue.toOption.value
      result must be(empty)
    }
  }

  "storeCredentialPublicationData" should {
    "insert credential data in db" in {
      val issuerId = createIssuer("Issuer X")
      val group = createIssuerGroup(issuerId, IssuerGroup.Name("grp1"))
      val subjectId = createContact(issuerId, "IOHK Student 2", group.name).contactId
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
        .storeCredentialPublicationData(
          issuerId,
          CredentialPublicationData(
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
      val issuerId = createIssuer("Issuer X")

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
          .storeCredentialPublicationData(
            issuerId,
            CredentialPublicationData(
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
      val issuerId = createIssuer("Issuer X")
      val group = createIssuerGroup(issuerId, IssuerGroup.Name("grp1"))
      val subjectId = createContact(issuerId, "IOHK Student 2", group.name).contactId
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
          .storeCredentialPublicationData(
            Institution.Id.random(),
            CredentialPublicationData(
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
      val issuerId = createIssuer("Issuer X")
      val group = createIssuerGroup(issuerId, IssuerGroup.Name("grp1"))
      val subjectId = createContact(issuerId, "IOHK Student 2", group.name).contactId
      val originalCredential = createGenericCredential(issuerId, subjectId, "A")

      val mockHash = SHA256Digest.compute("000".getBytes())
      val mockBatchId = CredentialBatchId.fromDigest(mockHash)
      val mockEncodedSignedCredential = "easdadgfkfñwlekrjfadf"
      val mockMerkleProof = MerkleInclusionProof(mockHash, 1, List(mockHash))

      intercept[RuntimeException](
        credentialsRepository
          .storeCredentialPublicationData(
            issuerId,
            CredentialPublicationData(
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

  "storeBatchData" should {
    "insert the expected data" in {
      val mockHash = SHA256Digest.compute("random".getBytes())
      val mockBatchId = CredentialBatchId.fromDigest(mockHash)
      val mockLedger = Ledger.InMemory
      val mockTransactionId =
        TransactionId.from("1423856bc91c49e928f6f30f4e8d665d53eb4ab6028bd0ac971809d514c92db1").value

      credentialsRepository
        .storeBatchData(
          StoreBatchData(mockBatchId, mockHash, TransactionInfo(mockTransactionId, mockLedger, None))
        )
        .value
        .futureValue

      val (transactionId, ledger, hash) = DataPreparation.getBatchData(mockBatchId).value

      transactionId mustBe mockTransactionId
      ledger mustBe mockLedger
      hash mustBe mockHash
    }
  }

  "storeRevocationData" should {
    "store the related transaction id" in {
      val institutionId = createIssuer("Issuer X")
      val contactId = createContact(institutionId, "IOHK Student 2", None, "").contactId
      val credential = createGenericCredential(institutionId, contactId, "A")

      val mockOperationHash = SHA256Digest.compute("000".getBytes())
      val mockBatchId = CredentialBatchId.fromDigest(mockOperationHash)
      val mockTransactionId = TransactionId
        .from("1423856bc91c49e928f6f30f4e8d665d53eb4ab6028bd0ac971809d514c92db1")
        .value
      val mockLedger = Ledger.InMemory
      val mockTransactionInfo = TransactionInfo(mockTransactionId, mockLedger)

      // we first publish the batch
      DataPreparation.publishBatch(mockBatchId, mockOperationHash, mockTransactionInfo)

      val mockEncodedSignedCredential = "easdadgfkfñwlekrjfadf"
      val mockMerkleProof = MerkleInclusionProof(mockOperationHash, 1, List(mockOperationHash))

      credentialsRepository
        .storeCredentialPublicationData(
          institutionId,
          CredentialPublicationData(
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
      val institutionId = createIssuer("Issuer X")
      val contactId = createContact(institutionId, "IOHK Student 2", None, "").contactId
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

    "fail when the credential doesn't does not belong to the institution" in {
      val institutionId = createIssuer("Issuer X")
      val contactId = createContact(institutionId, "IOHK Student 2", None, "").contactId
      val credential = createGenericCredential(institutionId, contactId, "A")

      val mockOperationHash = SHA256Digest.compute("000".getBytes())
      val mockBatchId = CredentialBatchId.fromDigest(mockOperationHash)
      val mockTransactionId = TransactionId
        .from("1423856bc91c49e928f6f30f4e8d665d53eb4ab6028bd0ac971809d514c92db1")
        .value
      val mockLedger = Ledger.InMemory
      val mockTransactionInfo = TransactionInfo(mockTransactionId, mockLedger)

      // we first publish the batch
      DataPreparation.publishBatch(mockBatchId, mockOperationHash, mockTransactionInfo)

      val mockEncodedSignedCredential = "easdadgfkfñwlekrjfadf"
      val mockMerkleProof = MerkleInclusionProof(mockOperationHash, 1, List(mockOperationHash))

      credentialsRepository
        .storeCredentialPublicationData(
          institutionId,
          CredentialPublicationData(
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
          .storeRevocationData(createIssuer("Issuer Y"), credential.credentialId, revocationTransactionId)
          .value
          .futureValue
      }
    }
  }

  "markAsShared" should {
    "work" in {
      val issuerId = createIssuer("Issuer X")
      val subjectId = createContact(issuerId, "IOHK Student", None, "").contactId
      val credential = createGenericCredential(issuerId, subjectId, "A")
      publish(issuerId, credential.credentialId)
      credentialsRepository.markAsShared(issuerId, credential.credentialId).value.futureValue.toOption.value

      val result = credentialsRepository.getBy(credential.credentialId).value.futureValue.toOption.value.value
      result.sharedAt mustNot be(empty)
    }

    "set a new date even if the credential was shared before" in {
      val issuerId = createIssuer("Issuer X")
      val subjectId = createContact(issuerId, "IOHK Student", None, "").contactId
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
      val issuerId = createIssuer("Issuer X")
      val subjectId = createContact(issuerId, "IOHK Student", None, "").contactId
      val credential = createGenericCredential(issuerId, subjectId, "A")
      assertThrows[Exception] {
        credentialsRepository.markAsShared(issuerId, credential.credentialId).value.futureValue.toOption.value
      }
    }

    "fail when the credential doesn't belong to the given issuer" in {
      val issuerId = createIssuer("Issuer X")
      val issuerId2 = createIssuer("Issuer Y")
      val subjectId = createContact(issuerId, "IOHK Student", None, "").contactId
      val credential = createGenericCredential(issuerId, subjectId, "A")
      publish(issuerId, credential.credentialId)
      assertThrows[Exception] {
        credentialsRepository.markAsShared(issuerId2, credential.credentialId).value.futureValue.toOption.value
      }
    }
  }

  private def publish(
      issuerId: Institution.Id,
      consoleId: GenericCredential.Id
  ): Unit = {
    DataPreparation.publishBatch(aBatchId, aHash, aTxInfo)
    DataPreparation.publishCredential(issuerId, aBatchId, consoleId, anEncodedCred, aProof)
  }
}
