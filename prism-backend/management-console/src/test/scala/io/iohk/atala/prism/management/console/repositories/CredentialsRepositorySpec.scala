package io.iohk.atala.prism.management.console.repositories

import cats.data.NonEmptyList
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.syntax.functor._
import doobie.util.transactor
import doobie.implicits._
import io.circe.Json
import io.circe.syntax._
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.{MerkleInclusionProof, Sha256}
import io.iohk.atala.prism.management.console.DataPreparation
import io.iohk.atala.prism.management.console.DataPreparation._
import io.iohk.atala.prism.management.console.errors.{CredentialDataValidationFailed, PublishedCredentialsNotExist, PublishedCredentialsNotRevoked}
import io.iohk.atala.prism.management.console.models._
import org.scalatest.OptionValues._
import io.iohk.atala.prism.management.console.repositories.daos.CredentialTypeDao
import io.iohk.atala.prism.utils.IOUtils._
import tofu.logging.Logs

import scala.jdk.CollectionConverters._

class CredentialsRepositorySpec extends AtalaWithPostgresSpec {
  import CredentialsRepositorySpec.publish

  val logs: Logs[IO, IO] = Logs.sync[IO, IO]

  lazy val credentialsRepository = CredentialsRepository.unsafe(database, logs)

  "create" should {
    "create a new credential" in {
      val issuerName = "Issuer-1"
      val contactName = "Student 1"
      val issuerId = createParticipant(issuerName)
      val group =
        createInstitutionGroup(issuerId, InstitutionGroup.Name("grp1"))
      val contact = createContact(issuerId, contactName, Some(group.name))
      val credentialTypeWithRequiredFields =
        DataPreparation.createCredentialType(issuerId, "name")
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

      val result =
        credentialsRepository.create(issuerId, request).unsafeRunSync()
      val credential = result.toOption.value
      credential.credentialData must be(request.credentialData)
      credential.issuedBy must be(issuerId)
      credential.contactId must be(request.contactId.value)
      credential.issuerName must be(issuerName)
      credential.contactData must be(contact.data)
      credential.publicationData must be(empty)
    }

    "fail to create a new credential when referenced credential type can't be rendered with specified credential data" in {
      val issuerName = "Issuer-1"
      val contactName = "Student 1"
      val issuerId = createParticipant(issuerName)
      val contact = createContact(issuerId, contactName, None)
      val credentialTypeWithRequiredFields =
        DataPreparation.createCredentialType(issuerId, "name")
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

      val result =
        credentialsRepository.create(issuerId, request).unsafeRunSync()
      result mustBe a[Left[CredentialDataValidationFailed, _]]
    }
  }

  "getBy" should {
    "return the credential when found" in {
      val issuerId = createParticipant("Issuer X")
      val group =
        createInstitutionGroup(issuerId, InstitutionGroup.Name("grp1"))
      val contactId =
        createContact(issuerId, "IOHK Student 2", Some(group.name)).contactId
      val credential = createGenericCredential(
        issuerId,
        contactId,
        "A"
      )

      val returnedCredential =
        credentialsRepository
          .getBy(credential.credentialId)
          .unsafeRunSync()
          .value

      returnedCredential must be(credential)
    }

    "return a credential with publication data" in {
      val issuerId = createParticipant("Issuer X")
      val group =
        createInstitutionGroup(issuerId, InstitutionGroup.Name("grp1"))
      val contactId =
        createContact(issuerId, "IOHK Student 2", Some(group.name)).contactId
      val credential = createGenericCredential(issuerId, contactId, "A")

      publish(issuerId, credential.credentialId)

      credentialsRepository.getBy(credential.credentialId).unsafeRunSync().value
      succeed
    }

    "return no credential when not found" in {
      val credentialId = GenericCredential.Id.random()

      val credential = credentialsRepository.getBy(credentialId).unsafeRunSync()

      credential must be(None)
    }
  }

  "getBy" should {
    "return the first credentials" in {
      val issuerId = createParticipant("Issuer X")
      val group =
        createInstitutionGroup(issuerId, InstitutionGroup.Name("grp1"))
      val contact =
        createContact(issuerId, "IOHK Student", Some(group.name)).contactId
      val credA = createGenericCredential(issuerId, contact, "A")
      val credB = createGenericCredential(issuerId, contact, "B")
      createGenericCredential(issuerId, contact, "C")

      val result =
        credentialsRepository.getBy(issuerId, 2, None).unsafeRunSync()
      result.toSet must be(Set(credA, credB))
    }

    "return the first credentials involving a published one" in {
      val issuerId = createParticipant("Issuer X")
      val group =
        createInstitutionGroup(issuerId, InstitutionGroup.Name("grp1"))
      val contact =
        createContact(issuerId, "IOHK Student", Some(group.name)).contactId
      val credA = createGenericCredential(issuerId, contact, "A")
      val credB = createGenericCredential(issuerId, contact, "B")
      createGenericCredential(issuerId, contact, "C")
      publish(issuerId, credA.credentialId)
      credentialsRepository.getBy(credA.credentialId)

      val result =
        credentialsRepository.getBy(issuerId, 2, None).unsafeRunSync()
      result.map(_.credentialId).toSet must be(
        Set(credA.credentialId, credB.credentialId)
      )
    }

    "paginate by the last seen credential" in {
      val issuerId = createParticipant("Issuer X")
      val group =
        createInstitutionGroup(issuerId, InstitutionGroup.Name("grp1"))
      val contact =
        createContact(issuerId, "IOHK Student", Some(group.name)).contactId
      createGenericCredential(issuerId, contact, "A")
      createGenericCredential(issuerId, contact, "B")
      val credC = createGenericCredential(issuerId, contact, "C")
      createGenericCredential(issuerId, contact, "D")

      val first = credentialsRepository.getBy(issuerId, 2, None).unsafeRunSync()
      val result =
        credentialsRepository
          .getBy(issuerId, 1, first.lastOption.map(_.credentialId))
          .unsafeRunSync()
      result.toSet must be(Set(credC))
    }

    "paginate by the last seen credential including a published one" in {
      val issuerId = createParticipant("Issuer X")
      val group =
        createInstitutionGroup(issuerId, InstitutionGroup.Name("grp1"))
      val contact =
        createContact(issuerId, "IOHK Student", Some(group.name)).contactId
      createGenericCredential(issuerId, contact, "A")
      createGenericCredential(issuerId, contact, "B")
      val credC = createGenericCredential(issuerId, contact, "C")
      publish(issuerId, credC.credentialId)
      createGenericCredential(issuerId, contact, "D")

      val first = credentialsRepository.getBy(issuerId, 2, None).unsafeRunSync()
      val result =
        credentialsRepository
          .getBy(issuerId, 1, first.lastOption.map(_.credentialId))
          .unsafeRunSync()
      result.map(_.credentialId).toSet must be(Set(credC.credentialId))
    }

    "paginate with limit and use the query" in {
      val issuerId = createParticipant("Issuer X")
      val group =
        createInstitutionGroup(issuerId, InstitutionGroup.Name("grp1"))
      val subject =
        createContact(issuerId, "IOHK Student", Some(group.name)).contactId
      val credential1 = createGenericCredential(issuerId, subject, tag = "A")
      val credential2 = createGenericCredential(issuerId, subject, tag = "B")
      val credential3 = createGenericCredential(issuerId, subject, tag = "C")
      val credential4 = createGenericCredential(issuerId, subject, tag = "D")

      val query: GenericCredential.PaginatedQuery = PaginatedQueryConstraints(
        limit = 2,
        ordering = PaginatedQueryConstraints.ResultOrdering(
          GenericCredential.SortBy.CreatedOn,
          PaginatedQueryConstraints.ResultOrdering.Direction.Ascending
        )
      )

      credentialsRepository
        .getBy(issuerId, query)
        .unsafeToFuture()
        .futureValue mustBe List(
        credential1,
        credential2
      )

      val query2 = query.copy(offset = 2)

      credentialsRepository
        .getBy(issuerId, query2)
        .unsafeToFuture()
        .futureValue mustBe List(
        credential3,
        credential4
      )

      val credentialType =
        CredentialTypeDao
          .findCredentialType(issuerId, "Credential type B")
          .transact(database)
          .unsafeRunSync()
          .value

      val filters = GenericCredential.FilterBy(
        credentialType = Some(credentialType.id)
      )

      val query3 = query.copy(filters = Some(filters))

      credentialsRepository
        .getBy(issuerId, query3)
        .unsafeToFuture()
        .futureValue mustBe List(
        credential2
      )
    }
  }

  "getBy" should {
    "return contact's credentials" in {
      val issuerId = createParticipant("Issuer X")
      val group =
        createInstitutionGroup(issuerId, InstitutionGroup.Name("grp1"))
      val contactId1 =
        createContact(issuerId, "IOHK Student", Some(group.name)).contactId
      val contactId2 =
        createContact(issuerId, "IOHK Student 2", Some(group.name)).contactId
      createGenericCredential(issuerId, contactId2, "A")
      val cred1 = createGenericCredential(issuerId, contactId1, "B")
      createGenericCredential(issuerId, contactId2, "C")
      val cred2 = createGenericCredential(issuerId, contactId1, "D")
      createGenericCredential(issuerId, contactId2, "E")

      val result = credentialsRepository
        .getBy(issuerId, contactId1)
        .unsafeToFuture()
        .futureValue
      result must be(List(cred1, cred2))
    }

    "return contact's credentials including a published one" in {
      val issuerId = createParticipant("Issuer X")
      val group =
        createInstitutionGroup(issuerId, InstitutionGroup.Name("grp1"))
      val contactId1 =
        createContact(issuerId, "IOHK Student", Some(group.name)).contactId
      val contactId2 =
        createContact(issuerId, "IOHK Student 2", Some(group.name)).contactId
      createGenericCredential(issuerId, contactId2, "A")
      val cred1 = createGenericCredential(issuerId, contactId1, "B")
      createGenericCredential(issuerId, contactId2, "C")
      val cred2 = createGenericCredential(issuerId, contactId1, "D")
      createGenericCredential(issuerId, contactId2, "E")
      publish(issuerId, cred1.credentialId)

      val result = credentialsRepository
        .getBy(issuerId, contactId1)
        .unsafeToFuture()
        .futureValue
      result.map(_.credentialId) must be(
        List(cred1.credentialId, cred2.credentialId)
      )
    }

    "return empty list of credentials when not present" in {
      val issuerId = createParticipant("Issuer X")
      val group =
        createInstitutionGroup(issuerId, InstitutionGroup.Name("grp1"))
      val contactId =
        createContact(issuerId, "IOHK Student", Some(group.name)).contactId

      val result = credentialsRepository
        .getBy(issuerId, contactId)
        .unsafeToFuture()
        .futureValue
      result must be(empty)
    }
  }

  "storePublicationData" should {
    "insert credential data in db" in {
      val issuerId = createParticipant("Issuer X")
      val group =
        createInstitutionGroup(issuerId, InstitutionGroup.Name("grp1"))
      val contactId =
        createContact(issuerId, "IOHK Student 2", Some(group.name)).contactId
      val originalCredential = createGenericCredential(issuerId, contactId, "A")

      val mockOperationHash = Sha256.compute("000".getBytes())
      val mockBatchId = CredentialBatchId.fromDigest(mockOperationHash)

      // / we first publish the batch
      DataPreparation.publishBatch(
        mockBatchId,
        mockOperationHash,
        AtalaOperationId.fromVectorUnsafe(mockOperationHash.getValue.toVector)
      )

      val mockEncodedSignedCredential = "easdadgfkfñwlekrjfadf"
      val mockMerkleProof = new MerkleInclusionProof(
        mockOperationHash,
        1,
        List(mockOperationHash).asJava
      )

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
        .unsafeToFuture()
        .futureValue

      inserted must be(1)

      val credentialList =
        credentialsRepository
          .getBy(issuerId, contactId)
          .unsafeToFuture()
          .futureValue

      credentialList.length must be(1)

      val updatedCredential = credentialList.headOption.value
      val publicationData = updatedCredential.publicationData.value

      publicationData.credentialBatchId must be(mockBatchId)
      publicationData.issuanceOperationHash must be(mockOperationHash)
      publicationData.encodedSignedCredential must be(
        mockEncodedSignedCredential
      )
      // the rest should remain unchanged
      updatedCredential.copy(publicationData = None) must be(originalCredential)
    }

    "fail when credential_id is not registered" in {
      val issuerId = createParticipant("Issuer X")

      val mockOperationHash = Sha256.compute("000".getBytes())
      val mockBatchId = CredentialBatchId.fromDigest(mockOperationHash)

      // / we first publish the batch
      DataPreparation.publishBatch(
        mockBatchId,
        mockOperationHash,
        AtalaOperationId.fromVectorUnsafe(mockOperationHash.getValue.toVector)
      )

      val mockEncodedSignedCredential = "easdadgfkfñwlekrjfadf"
      val mockMerkleProof = new MerkleInclusionProof(
        mockOperationHash,
        1,
        List(mockOperationHash).asJava
      )

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
          .unsafeToFuture()
          .futureValue
      )

      val credentialList =
        credentialsRepository
          .getBy(issuerId, 10, None)
          .unsafeToFuture()
          .futureValue

      credentialList must be(empty)
    }

    "fail when issuer_id does not belong to the credential_id" in {
      val issuerId = createParticipant("Issuer X")
      val group =
        createInstitutionGroup(issuerId, InstitutionGroup.Name("grp1"))
      val contactId =
        createContact(issuerId, "IOHK Student 2", Some(group.name)).contactId
      val originalCredential = createGenericCredential(issuerId, contactId, "A")

      val mockOperationHash = Sha256.compute("000".getBytes())
      val mockBatchId = CredentialBatchId.fromDigest(mockOperationHash)

      // / we first publish the batch
      DataPreparation.publishBatch(
        mockBatchId,
        mockOperationHash,
        AtalaOperationId.fromVectorUnsafe(mockOperationHash.getValue.toVector)
      )

      val mockEncodedSignedCredential = "easdadgfkfñwlekrjfadf"
      val mockMerkleProof = new MerkleInclusionProof(
        mockOperationHash,
        1,
        List(mockOperationHash).asJava
      )

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
          .unsafeToFuture()
          .futureValue
      )

      val credentialList =
        credentialsRepository
          .getBy(issuerId, contactId)
          .unsafeToFuture()
          .futureValue

      credentialList.length must be(1)

      val updatedCredential = credentialList.headOption.value

      updatedCredential must be(originalCredential)
      updatedCredential.publicationData must be(empty)
    }

    "fail when the batch id does not exist" in {
      val issuerId = createParticipant("Issuer X")
      val group =
        createInstitutionGroup(issuerId, InstitutionGroup.Name("grp1"))
      val contactId =
        createContact(issuerId, "IOHK Student 2", Some(group.name)).contactId
      val originalCredential = createGenericCredential(issuerId, contactId, "A")

      val mockHash = Sha256.compute("000".getBytes())
      val mockBatchId = CredentialBatchId.fromDigest(mockHash)
      val mockEncodedSignedCredential = "easdadgfkfñwlekrjfadf"
      val mockMerkleProof =
        new MerkleInclusionProof(mockHash, 1, List(mockHash).asJava)

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
          .unsafeToFuture()
          .futureValue
      )

      val credentialList =
        credentialsRepository
          .getBy(issuerId, contactId)
          .unsafeToFuture()
          .futureValue

      credentialList.length must be(1)

      val updatedCredential = credentialList.headOption.value

      updatedCredential must be(originalCredential)
      updatedCredential.publicationData must be(empty)
    }
  }

  "markAsShared" should {
    "work" in {
      val issuerId = createParticipant("Issuer X")
      val contactId = createContact(issuerId, "IOHK Student", None).contactId

      val credential1 = createGenericCredential(issuerId, contactId, "A")
      val credential2 = createGenericCredential(issuerId, contactId, "B")

      publishCredential(issuerId, credential1)
      publishCredential(issuerId, credential2)

      credentialsRepository
        .markAsShared(
          issuerId,
          NonEmptyList.of(credential1.credentialId, credential2.credentialId)
        )
        .unsafeToFuture()
        .futureValue

      val result1 = credentialsRepository
        .getBy(credential1.credentialId)
        .unsafeToFuture()
        .futureValue
        .value
      result1.sharedAt mustNot be(empty)

      val result2 = credentialsRepository
        .getBy(credential2.credentialId)
        .unsafeToFuture()
        .futureValue
        .value
      result2.sharedAt mustNot be(empty)
    }

    "set a new date even if the credential was shared before" in {
      val issuerId = createParticipant("Issuer X")
      val contactId = createContact(issuerId, "IOHK Student", None).contactId
      val credential = createGenericCredential(issuerId, contactId, "A")

      publish(issuerId, credential.credentialId)
      credentialsRepository
        .markAsShared(issuerId, NonEmptyList.of(credential.credentialId))
        .unsafeToFuture()
        .futureValue

      val result1 = credentialsRepository
        .getBy(credential.credentialId)
        .unsafeToFuture()
        .futureValue
        .value
        .sharedAt
        .value

      credentialsRepository
        .markAsShared(issuerId, NonEmptyList.of(credential.credentialId))
        .unsafeToFuture()
        .futureValue
      val result2 = credentialsRepository
        .getBy(credential.credentialId)
        .unsafeToFuture()
        .futureValue
        .value
        .sharedAt
        .value
      (result2.compareTo(result1) > 0) must be(true)
    }

    "fail when the credential hasn't been published" in {
      val issuerId = createParticipant("Issuer X")
      val contactId = createContact(issuerId, "IOHK Student", None).contactId
      val credential = createGenericCredential(issuerId, contactId, "A")
      assertThrows[Exception] {
        credentialsRepository
          .markAsShared(issuerId, NonEmptyList.of(credential.credentialId))
          .unsafeToFuture()
          .futureValue
      }
    }

    "fail when the credential doesn't belong to the given issuer" in {
      val issuerId = createParticipant("Issuer X")
      val issuerId2 = createParticipant("Issuer Y")
      val contactId = createContact(issuerId, "IOHK Student", None).contactId
      val credential = createGenericCredential(issuerId, contactId, "A")
      publish(issuerId, credential.credentialId)
      assertThrows[Exception] {
        credentialsRepository
          .markAsShared(issuerId2, NonEmptyList.of(credential.credentialId))
          .unsafeToFuture()
          .futureValue
      }
    }
  }

  "verifyPublishedCredentialsExist" should {
    "work" in {
      val issuerId = createParticipant("Issuer X")
      val contactId = createContact(issuerId, "IOHK Student", None).contactId

      val credential1 = createGenericCredential(issuerId, contactId, "A")
      val credential2 = createGenericCredential(issuerId, contactId, "B")

      publishCredential(issuerId, credential1)
      publishCredential(issuerId, credential2)

      val result = credentialsRepository
        .verifyPublishedCredentialsExist(
          issuerId,
          NonEmptyList.of(credential1.credentialId, credential2.credentialId)
        )
        .unsafeToFuture()
        .futureValue

      result mustBe a[Right[_, _]]
    }

    "return error when one of the credential is not found or published" in {
      val issuerId = createParticipant("Issuer X")
      val contactId = createContact(issuerId, "IOHK Student", None).contactId

      val credential1 = createGenericCredential(issuerId, contactId, "A")
      val credential2 = createGenericCredential(issuerId, contactId, "B")

      publishCredential(issuerId, credential1)

      val result = credentialsRepository
        .verifyPublishedCredentialsExist(
          issuerId,
          NonEmptyList.of(credential1.credentialId, credential2.credentialId)
        )
        .unsafeToFuture()
        .futureValue

      result mustBe Left(
        PublishedCredentialsNotExist(List(credential2.credentialId))
      )
    }

  }

  "storeBatchData" should {
    "insert the expected data" in {
      val mockHash = Sha256.compute("randomizer2021".getBytes())
      val mockBatchId = CredentialBatchId.fromDigest(mockHash)

      val added = credentialsRepository
        .storeBatchData(
          mockBatchId,
          mockHash,
          AtalaOperationId.fromVectorUnsafe(mockHash.getValue.toVector)
        )
        .unsafeToFuture()
        .futureValue

      added must be(1)
      val (operationId, hash) = DataPreparation.getBatchData(mockBatchId).value

      hash mustBe mockHash
      operationId.digest mustBe mockHash
    }
  }

  "deleteCredentials" should {
    "delete draft and published, revoked credentials" in {
      val issuerId = createParticipant("Issuer X")
      val contactId = createContact(issuerId, "IOHK Student", None).contactId

      val credential1 = createGenericCredential(issuerId, contactId, "A")
      val credential2 = createGenericCredential(issuerId, contactId, "B")

      publishCredential(issuerId, credential1)
      markAsRevoked(credential1.credentialId)

      val result = credentialsRepository
        .deleteCredentials(
          issuerId,
          NonEmptyList.of(credential1.credentialId, credential2.credentialId)
        )
        .unsafeToFuture()
        .futureValue

      result mustBe a[Right[_, _]]
      credentialsRepository
        .getBy(credential1.credentialId)
        .unsafeToFuture()
        .futureValue mustBe None
      credentialsRepository
        .getBy(credential2.credentialId)
        .unsafeToFuture()
        .futureValue mustBe None
    }

    "do not delete credentials when one of them is published and not revoked" in {
      val issuerId = createParticipant("Issuer X")
      val contactId = createContact(issuerId, "IOHK Student", None).contactId

      val credential1 = createGenericCredential(issuerId, contactId, "A")
      val credential2 = createGenericCredential(issuerId, contactId, "B")

      publishCredential(issuerId, credential1)

      val result = credentialsRepository
        .deleteCredentials(
          issuerId,
          NonEmptyList.of(credential1.credentialId, credential2.credentialId)
        )
        .unsafeToFuture()
        .futureValue

      result mustBe Left(
        PublishedCredentialsNotRevoked(List(credential1.credentialId))
      )
      credentialsRepository
        .getBy(credential1.credentialId)
        .unsafeToFuture()
        .futureValue mustBe a[Some[_]]
      credentialsRepository
        .getBy(credential2.credentialId)
        .unsafeToFuture()
        .futureValue mustBe a[Some[_]]
    }

    def markAsRevoked(credentialId: GenericCredential.Id): Unit = {
      val operationId = 1.to(64).map(_ => "a").mkString("")
      sql"""
            |UPDATE published_credentials
            |SET revoked_on_operation_id = decode($operationId, 'hex')
            |WHERE credential_id = ${credentialId.uuid.toString}::uuid
       """.stripMargin.update.run.void.transact(database).unsafeRunSync()
    }
  }

  "storeRevocationData" should {
    "store the related transaction id" in {
      val institutionId = createParticipant("Issuer X")
      val contactId =
        createContact(institutionId, "IOHK Student 2", None).contactId
      val credential = createGenericCredential(institutionId, contactId, "A")

      val mockOperationHash = Sha256.compute("000".getBytes())
      val mockBatchId = CredentialBatchId.fromDigest(mockOperationHash)

      // we first publish the batch
      publishBatch(
        mockBatchId,
        mockOperationHash,
        AtalaOperationId.fromVectorUnsafe(mockOperationHash.getValue.toVector)
      )

      val mockEncodedSignedCredential = "easdadgfkfñwlekrjfadf"
      val mockMerkleProof = new MerkleInclusionProof(
        mockOperationHash,
        1,
        List(mockOperationHash).asJava
      )

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
        .unsafeToFuture()
        .futureValue must be(1)

      val revocationOperationId = AtalaOperationId
        .fromHexUnsafe(
          "98765432c91c49e928f6f30f4e8d665d53eb4ab6028bd0ac971809d514c92db1"
        )
      credentialsRepository
        .storeRevocationData(
          institutionId,
          credential.credentialId,
          revocationOperationId
        )
        .unsafeToFuture()
        .futureValue

      val result = credentialsRepository
        .getBy(institutionId, contactId)
        .unsafeToFuture()
        .futureValue
        .headOption
        .value

      result.revokedOnOperationId.value must be(revocationOperationId)
    }

    "fail when the credential doesn't exist" in {
      val institutionId = createParticipant("Issuer X")
      val contactId =
        createContact(institutionId, "IOHK Student 2", None).contactId
      createGenericCredential(institutionId, contactId, "A")

      val revocationOperationId = AtalaOperationId
        .fromHexUnsafe(
          "98765432c91c49e928f6f30f4e8d665d53eb4ab6028bd0ac971809d514c92db1"
        )

      intercept[RuntimeException] {
        credentialsRepository
          .storeRevocationData(
            institutionId,
            GenericCredential.Id.random(),
            revocationOperationId
          )
          .unsafeToFuture()
          .futureValue
      }
    }

    "fail when the credential does not belong to the institution" in {
      val institutionId = createParticipant("Issuer X")
      val contactId =
        createContact(institutionId, "IOHK Student 2", None).contactId
      val credential = createGenericCredential(institutionId, contactId, "A")

      val mockOperationHash = Sha256.compute("000".getBytes())
      val mockBatchId = CredentialBatchId.fromDigest(mockOperationHash)

      // we first publish the batch
      publishBatch(
        mockBatchId,
        mockOperationHash,
        AtalaOperationId.fromVectorUnsafe(mockOperationHash.getValue.toVector)
      )

      val mockEncodedSignedCredential = "easdadgfkfñwlekrjfadf"
      val mockMerkleProof = new MerkleInclusionProof(
        mockOperationHash,
        1,
        List(mockOperationHash).asJava
      )

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
        .unsafeToFuture()
        .futureValue must be(1)

      intercept[RuntimeException] {
        val revocationOperationId = AtalaOperationId
          .fromHexUnsafe(
            "98765432c91c49e928f6f30f4e8d665d53eb4ab6028bd0ac971809d514c92db1"
          )
        credentialsRepository
          .storeRevocationData(
            createParticipant("Issuer Y"),
            credential.credentialId,
            revocationOperationId
          )
          .unsafeToFuture()
          .futureValue
      }
    }
  }
}

object CredentialsRepositorySpec {
  private val aHash = Sha256.compute("random string".getBytes())
  private val aBatchId = CredentialBatchId.fromDigest(aHash)

  private val aProof = new MerkleInclusionProof(aHash, 1, List(aHash).asJava)
  private val anEncodedCred = "encodedSignedCredenital"

  def publish(
      issuerId: ParticipantId,
      consoleId: GenericCredential.Id
  )(implicit database: transactor.Transactor[IO]): Unit = {
    DataPreparation.publishBatch(
      aBatchId,
      aHash,
      AtalaOperationId.fromVectorUnsafe(aHash.getValue.toVector)
    )
    DataPreparation.publishCredential(
      issuerId,
      aBatchId,
      consoleId,
      anEncodedCred,
      aProof
    )
  }
}
