package io.iohk.atala.prism.console.repositories

import java.time.LocalDate
import java.util.UUID
import io.circe.Json
import io.circe.syntax._
import io.iohk.atala.prism.connector.model.ConnectionStatus
import io.iohk.atala.prism.console.DataPreparation._
import io.iohk.atala.prism.console.models.{
  CreateGenericCredential,
  GenericCredential,
  Institution,
  IssuerGroup,
  PublishCredential
}
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.models.{Ledger, TransactionId, TransactionInfo}
import io.iohk.atala.prism.AtalaWithPostgresSpec
import org.scalatest.OptionValues._

import scala.concurrent.duration._

class CredentialsRepositorySpec extends AtalaWithPostgresSpec {

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 5.millis)

  lazy val credentialsRepository = new CredentialsRepository(database)

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

      credentialsRepository.getBy(credential.credentialId).value.futureValue.toOption.value.value
      succeed
    }

    "return no credential when not found" in {
      val credentialId = GenericCredential.Id(UUID.randomUUID())

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
        credentialsRepository.getBy(issuerId, 10, None).value.futureValue.toOption.value

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
        credentialsRepository.getBy(issuerId, subjectId).value.futureValue.toOption.value

      credentialList.length must be(1)

      val updatedCredential = credentialList.headOption.value

      updatedCredential must be(originalCredential)
      updatedCredential.publicationData must be(empty)
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

  private def publish(issuerId: Institution.Id, id: GenericCredential.Id): Unit = {
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
