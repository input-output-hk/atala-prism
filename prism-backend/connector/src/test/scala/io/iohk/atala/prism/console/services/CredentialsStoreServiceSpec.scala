package io.iohk.atala.prism.console.services

import doobie.implicits._
import io.iohk.atala.prism.auth.SignedRpcRequest
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeaderParser
import io.iohk.atala.prism.connector.ConnectorAuthenticator
import io.iohk.atala.prism.connector.model.{ConnectionId, ParticipantLogo, ParticipantType}
import io.iohk.atala.prism.connector.repositories.ParticipantsRepository.CreateParticipantRequest
import io.iohk.atala.prism.connector.repositories.daos._
import io.iohk.atala.prism.connector.repositories.{ParticipantsRepository, RequestNoncesRepository}
import io.iohk.atala.prism.console.DataPreparation
import io.iohk.atala.prism.console.models.{Contact, CredentialExternalId, Institution}
import io.iohk.atala.prism.console.repositories.StoredCredentialsRepository
import io.iohk.atala.prism.console.repositories.daos.{ContactsDAO, StoredCredentialsDAO}
import io.iohk.atala.prism.crypto.{EC, ECKeyPair, SHA256Digest}
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.models.{Ledger, ParticipantId, TransactionId, TransactionInfo}
import io.iohk.atala.prism.protos.cstore_api
import io.iohk.atala.prism.{DIDGenerator, RpcSpecBase}
import org.mockito.MockitoSugar._
import org.scalatest.OptionValues._

import java.util.UUID

class CredentialsStoreServiceSpec extends RpcSpecBase with DIDGenerator {
  val usingApiAs = usingApiAsConstructor(
    new cstore_api.CredentialsStoreServiceGrpc.CredentialsStoreServiceBlockingStub(_, _)
  )

  lazy val storedCredentials = new StoredCredentialsRepository(database)
  private lazy val participantsRepository = new ParticipantsRepository(database)(executionContext)
  private lazy val requestNoncesRepository = new RequestNoncesRepository.PostgresImpl(database)(executionContext)
  protected lazy val nodeMock = mock[io.iohk.atala.prism.protos.node_api.NodeServiceGrpc.NodeService]

  private lazy val authenticator = new ConnectorAuthenticator(
    participantsRepository,
    requestNoncesRepository,
    nodeMock,
    GrpcAuthenticationHeaderParser
  )
  lazy val verifierId = ParticipantId("af45a4da-65b8-473e-aadc-aa6b346250a3")

  override def services =
    Seq(
      cstore_api.CredentialsStoreServiceGrpc
        .bindService(
          new CredentialsStoreService(storedCredentials, authenticator),
          executionContext
        )
    )

  override def beforeEach(): Unit = {
    super.beforeEach()

    new ParticipantsRepository(database)
      .create(
        CreateParticipantRequest(
          verifierId,
          ParticipantType.Verifier,
          "Verifier",
          DID.buildPrismDID("test"),
          ParticipantLogo(Vector()),
          TransactionInfo(TransactionId.from(SHA256Digest.compute("id".getBytes).value).value, Ledger.InMemory)
        )
      )
      .value
      .futureValue
    ()
  }

  def updateDid(participantId: ParticipantId, did: DID): doobie.ConnectionIO[Unit] = {
    sql"""
         |UPDATE participants
         |SET did = $did
         |WHERE id = $participantId
       """.stripMargin.update.run.map(_ => ())
  }

  def storeCredentialFor(
      keyPair: ECKeyPair,
      did: DID,
      verifierId: ParticipantId,
      connectionId: ConnectionId,
      contactId: Contact.Id,
      encodedSignedCredential: String
  ): Unit = {
    val connectionToken =
      DataPreparation.generateConnectionToken(Institution.Id(verifierId.uuid), contactId)
    ContactsDAO
      .setConnectionAsAccepted(Institution.Id(verifierId.uuid), connectionToken, connectionId)
      .transact(database)
      .unsafeToFuture()
      .futureValue

    val mockCredentialExternalId = CredentialExternalId.random()
    val storeRequest = cstore_api.StoreCredentialRequest(
      connectionId.id.toString,
      encodedSignedCredential,
      mockCredentialExternalId.value
    )
    val rpcStoreRequest = SignedRpcRequest.generate(keyPair, did, storeRequest)
    usingApiAs(rpcStoreRequest) { serviceStub =>
      serviceStub.storeCredential(storeRequest)
    }
    ()
  }

  "storeCredential" should {
    "store credential in the database" in {
      lazy val keyPair = EC.generateKeyPair()
      lazy val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      updateDid(verifierId, did).transact(database).unsafeRunSync()

      val contactId = DataPreparation.createContact(Institution.Id(verifierId.uuid), "Individual", None, "").contactId
      val connectionToken = DataPreparation.generateConnectionToken(Institution.Id(verifierId.uuid), contactId)
      val mockConnectionId = ConnectionId(UUID.randomUUID())
      ContactsDAO
        .setConnectionAsAccepted(Institution.Id(verifierId.uuid), connectionToken, mockConnectionId)
        .transact(database)
        .unsafeToFuture()
        .futureValue

      val encodedSignedCredential = "a3cacb2d9e51bdd40264b287db15b4121ddee84eafb8c3da545c88c1d99b94d4"
      val mockCredentialExternalId = CredentialExternalId.random()
      val request =
        cstore_api.StoreCredentialRequest(
          mockConnectionId.id.toString,
          encodedSignedCredential,
          mockCredentialExternalId.value
        )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        serviceStub.storeCredential(request)

        val credential = StoredCredentialsDAO
          .getStoredCredentialsFor(Institution.Id(verifierId.uuid), Some(contactId))
          .transact(database)
          .unsafeToFuture()
          .futureValue
          .head

        credential.individualId mustBe contactId
        credential.encodedSignedCredential mustBe encodedSignedCredential
      }
    }

    "NOT store credential in the database when there is a message id conflict" in {
      lazy val keyPair = EC.generateKeyPair()
      lazy val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      updateDid(verifierId, did).transact(database).unsafeRunSync()

      val contactId = DataPreparation.createContact(Institution.Id(verifierId.uuid), "Individual", None, "").contactId
      val connectionToken = DataPreparation.generateConnectionToken(Institution.Id(verifierId.uuid), contactId)
      val mockConnectionId = ConnectionId(UUID.randomUUID())
      ContactsDAO
        .setConnectionAsAccepted(Institution.Id(verifierId.uuid), connectionToken, mockConnectionId)
        .transact(database)
        .unsafeToFuture()
        .futureValue

      val encodedSignedCredential = "a3cacb2d9e51bdd40264b287db15b4121ddee84eafb8c3da545c88c1d99b94d4"
      val mockCredentialExternalId = CredentialExternalId.random()
      val request =
        cstore_api.StoreCredentialRequest(
          mockConnectionId.id.toString,
          encodedSignedCredential,
          mockCredentialExternalId.value
        )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        serviceStub.storeCredential(request)

        val credential = StoredCredentialsDAO
          .getStoredCredentialsFor(Institution.Id(verifierId.uuid), Some(contactId))
          .transact(database)
          .unsafeToFuture()
          .futureValue
          .head

        credential.individualId mustBe contactId
        credential.encodedSignedCredential mustBe encodedSignedCredential
      }

      val encodedSignedCredential2 = "b3cacb2d9e51bdd40264b287db15b4121ddee84eafb8c3da545c88c1d99b94d4"
      val request2 =
        cstore_api.StoreCredentialRequest(
          mockConnectionId.id.toString,
          encodedSignedCredential2,
          mockCredentialExternalId.value
        )
      val rpcRequest2 = SignedRpcRequest.generate(keyPair, did, request2)

      usingApiAs(rpcRequest2) { serviceStub =>
        serviceStub.storeCredential(request2)

        val credentials = StoredCredentialsDAO
          .getStoredCredentialsFor(Institution.Id(verifierId.uuid), Some(contactId))
          .transact(database)
          .unsafeToFuture()
          .futureValue

        credentials.size must be(1)

        val credential = credentials.head

        credential.individualId mustBe contactId
        // the credential should be the original one
        credential.encodedSignedCredential mustBe encodedSignedCredential
      }

    }
  }

  "getStoredCredentialsFor" should {
    "get credentials for a specific contact" in {
      lazy val keyPair = EC.generateKeyPair()
      lazy val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      updateDid(verifierId, did).transact(database).unsafeRunSync()

      val contact1 = DataPreparation.createContact(Institution.Id(verifierId.uuid), "Individual 1", None, "")
      val contactId1 = contact1.contactId
      val contact2 = DataPreparation.createContact(Institution.Id(verifierId.uuid), "Individual 2", None, "")
      val contactId2 = contact2.contactId

      val encodedSignedCredential1 = "1a3cacb2d9e51bdd40264b287db15b4121ddee84eafb8c3da545c88c1d99b94d4"
      val encodedSignedCredential2 = "2a3cacb2d9e51bdd40264b287db15b4121ddee84eafb8c3da545c88c1d99b94d4"
      val connectionId1 = ConnectionId(UUID.randomUUID())
      val connectionId2 = ConnectionId(UUID.randomUUID())

      storeCredentialFor(keyPair, did, verifierId, connectionId1, contactId1, encodedSignedCredential1)
      storeCredentialFor(keyPair, did, verifierId, connectionId2, contactId2, encodedSignedCredential2)

      val getStoredRequest = cstore_api.GetStoredCredentialsForRequest(contactId1.value.toString)
      val rpcGetStoreRequest = SignedRpcRequest.generate(keyPair, did, getStoredRequest)
      usingApiAs(rpcGetStoreRequest) { serviceStub =>
        val response = serviceStub.getStoredCredentialsFor(getStoredRequest)

        response.credentials.size mustBe 1
        val credential = response.credentials.head
        credential.individualId mustBe contactId1.value.toString
        credential.encodedSignedCredential mustBe encodedSignedCredential1
        credential.externalId mustBe contact1.externalId.value
      }
    }

    "get credentials for all contacts" in {
      lazy val keyPair = EC.generateKeyPair()
      lazy val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      updateDid(verifierId, did).transact(database).unsafeRunSync()

      val contact1 = DataPreparation.createContact(Institution.Id(verifierId.uuid), "Individual 1", None, "")
      val contactId1 = contact1.contactId
      val contact2 = DataPreparation.createContact(Institution.Id(verifierId.uuid), "Individual 2", None, "")
      val contactId2 = contact2.contactId
      val encodedSignedCredential1 = "1a3cacb2d9e51bdd40264b287db15b4121ddee84eafb8c3da545c88c1d99b94d4"
      val encodedSignedCredential2 = "2a3cacb2d9e51bdd40264b287db15b4121ddee84eafb8c3da545c88c1d99b94d4"
      val connectionId1 = ConnectionId(UUID.randomUUID())
      val connectionId2 = ConnectionId(UUID.randomUUID())

      storeCredentialFor(keyPair, did, verifierId, connectionId1, contactId1, encodedSignedCredential1)
      storeCredentialFor(keyPair, did, verifierId, connectionId2, contactId2, encodedSignedCredential2)

      val getStoredRequest = cstore_api.GetStoredCredentialsForRequest()
      val rpcGetStoreRequest = SignedRpcRequest.generate(keyPair, did, getStoredRequest)
      usingApiAs(rpcGetStoreRequest) { serviceStub =>
        val response = serviceStub.getStoredCredentialsFor(getStoredRequest)

        response.credentials.size mustBe 2

        val credentials = response.credentials.map { cred =>
          (cred.individualId, cred.encodedSignedCredential, cred.externalId)
        }

        credentials must contain theSameElementsAs List(
          (contactId1.value.toString, encodedSignedCredential1, contact1.externalId.value),
          (contactId2.value.toString, encodedSignedCredential2, contact2.externalId.value)
        )
      }
    }
  }

  "getLatestCredentialExternalId" should {
    "return the last id stored" in {
      lazy val keyPair = EC.generateKeyPair()
      lazy val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      updateDid(verifierId, did).transact(database).unsafeRunSync()

      val contactId = DataPreparation.createContact(Institution.Id(verifierId.uuid), "Individual", None, "").contactId

      val connectionToken =
        DataPreparation.generateConnectionToken(Institution.Id(verifierId.uuid), contactId)
      val mockConnectionId = ConnectionId(UUID.randomUUID())
      ContactsDAO
        .setConnectionAsAccepted(Institution.Id(verifierId.uuid), connectionToken, mockConnectionId)
        .transact(database)
        .unsafeToFuture()
        .futureValue

      // we generate 10 new ids
      val credentialExternalIds = for (_ <- 1 to 10) yield CredentialExternalId.random()
      val encodedSignedCredential = "a3cacb2d9e51bdd40264b287db15b4121ddee84eafb8c3da545c88c1d99b94d4"
      val storeRequests = credentialExternalIds.map { messageId =>
        cstore_api.StoreCredentialRequest(
          mockConnectionId.id.toString,
          encodedSignedCredential,
          messageId.value
        )
      }

      storeRequests map { storeRequest =>
        val rpcStoreRequest = SignedRpcRequest.generate(keyPair, did, storeRequest)
        usingApiAs(rpcStoreRequest) { serviceStub =>
          serviceStub.storeCredential(storeRequest)
        }
      }

      val getLastStoredMessageIdRequest = cstore_api.GetLatestCredentialExternalIdRequest()
      val rpcGetStoreRequest = SignedRpcRequest.generate(keyPair, did, getLastStoredMessageIdRequest)
      usingApiAs(rpcGetStoreRequest) { serviceStub =>
        val response = serviceStub.getLatestCredentialExternalId(getLastStoredMessageIdRequest)

        response.latestCredentialExternalId mustBe credentialExternalIds.last.value
      }
    }
  }
}
