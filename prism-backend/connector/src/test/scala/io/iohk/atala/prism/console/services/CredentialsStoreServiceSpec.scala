package io.iohk.atala.prism.console.services

import cats.syntax.functor._
import cats.syntax.option._
import doobie.implicits._
import io.iohk.atala.prism.auth.SignedRpcRequest
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeaderParser
import io.iohk.atala.prism.connector.{AtalaOperationId, ConnectorAuthenticator}
import io.iohk.atala.prism.connector.model.{ConnectionId, ParticipantLogo, ParticipantType}
import io.iohk.atala.prism.connector.repositories.ParticipantsRepository.CreateParticipantRequest
import io.iohk.atala.prism.connector.repositories.daos._
import io.iohk.atala.prism.connector.repositories.{ParticipantsRepository, RequestNoncesRepository}
import io.iohk.atala.prism.console.DataPreparation
import io.iohk.atala.prism.console.models.{Contact, CredentialExternalId, Institution}
import io.iohk.atala.prism.console.repositories.StoredCredentialsRepository
import io.iohk.atala.prism.console.repositories.daos.{ContactsDAO, ReceivedCredentialsDAO}
import io.iohk.atala.prism.crypto.MerkleTree.MerkleInclusionProof
import io.iohk.atala.prism.crypto.{ECKeyPair, SHA256Digest}
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.protos.console_api
import io.iohk.atala.prism.{DIDUtil, RpcSpecBase}
import org.mockito.MockitoSugar._
import org.scalatest.Assertion

class CredentialsStoreServiceSpec extends RpcSpecBase with DIDUtil {
  val usingApiAs = usingApiAsConstructor(
    new console_api.CredentialsStoreServiceGrpc.CredentialsStoreServiceBlockingStub(_, _)
  )

  lazy val storedCredentials = new StoredCredentialsRepository(database)
  private lazy val participantsRepository = new ParticipantsRepository(database)
  private lazy val requestNoncesRepository = new RequestNoncesRepository.PostgresImpl(database)(executionContext)
  protected lazy val nodeMock = mock[io.iohk.atala.prism.protos.node_api.NodeServiceGrpc.NodeService]
  private val aHash = SHA256Digest.compute("".getBytes())
  private val inclusionProof = MerkleInclusionProof(aHash, 0, List())

  private lazy val authenticator = new ConnectorAuthenticator(
    participantsRepository,
    requestNoncesRepository,
    nodeMock,
    GrpcAuthenticationHeaderParser
  )
  lazy val verifierId = ParticipantId.unsafeFrom("af45a4da-65b8-473e-aadc-aa6b346250a3")

  override def services =
    Seq(
      console_api.CredentialsStoreServiceGrpc
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
          DataPreparation.newDID(),
          ParticipantLogo(Vector()),
          AtalaOperationId.random().some
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
       """.stripMargin.update.run.void
  }

  def storeCredentialFor(
      keyPair: ECKeyPair,
      did: DID,
      verifierId: ParticipantId,
      connectionId: ConnectionId,
      contactId: Contact.Id,
      encodedSignedCredential: String,
      proof: MerkleInclusionProof
  ): Unit = {
    val connectionToken =
      DataPreparation.generateConnectionToken(Institution.Id(verifierId.uuid), contactId)
    ContactsDAO
      .setConnectionAsAccepted(Institution.Id(verifierId.uuid), connectionToken, connectionId)
      .transact(database)
      .unsafeToFuture()
      .futureValue

    val mockCredentialExternalId = CredentialExternalId.random()
    val storeRequest = console_api.StoreCredentialRequest(
      connectionId.toString,
      encodedSignedCredential,
      mockCredentialExternalId.value,
      proof.encode
    )
    val rpcStoreRequest = SignedRpcRequest.generate(keyPair, did, storeRequest)
    usingApiAs(rpcStoreRequest) { serviceStub =>
      serviceStub.storeCredential(storeRequest)
    }
    ()
  }

  "storeCredential" should {
    "store credential in the database" in {
      val (keyPair, did) = createDid
      testStoringCredentialInDB(verifierId, keyPair, did)
    }
    "store credential in the database while use unpublished did" in {
      val (keyPair, did) = DIDUtil.createUnpublishedDid
      val verifierUnpubId = DataPreparation.createVerifier()
      testStoringCredentialInDB(verifierUnpubId, keyPair, did)
    }

    "NOT store credential in the database when there is a message id conflict" in {
      val (keyPair, did) = createDid
      updateDid(verifierId, did).transact(database).unsafeRunSync()

      val contact = DataPreparation.createContact(Institution.Id(verifierId.uuid), "Individual", None, "")
      val contactId = contact.contactId
      val contactExternalId = contact.externalId
      val connectionToken = DataPreparation.generateConnectionToken(Institution.Id(verifierId.uuid), contactId)
      val mockConnectionId = ConnectionId.random()
      ContactsDAO
        .setConnectionAsAccepted(Institution.Id(verifierId.uuid), connectionToken, mockConnectionId)
        .transact(database)
        .unsafeToFuture()
        .futureValue

      val encodedSignedCredential = "a3cacb2d9e51bdd40264b287db15b4121ddee84eafb8c3da545c88c1d99b94d4"
      val mockCredentialExternalId = CredentialExternalId.random()
      val request =
        console_api.StoreCredentialRequest(
          mockConnectionId.toString,
          encodedSignedCredential,
          mockCredentialExternalId.value,
          inclusionProof.encode
        )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        serviceStub.storeCredential(request)

        val credential = ReceivedCredentialsDAO
          .getReceivedCredentialsFor(Institution.Id(verifierId.uuid), Some(contactId))
          .transact(database)
          .unsafeToFuture()
          .futureValue
          .head

        credential.individualId mustBe contactId
        credential.encodedSignedCredential mustBe encodedSignedCredential
        credential.externalId mustBe contactExternalId
        credential.merkleInclusionProof mustBe inclusionProof
      }

      val encodedSignedCredential2 = "b3cacb2d9e51bdd40264b287db15b4121ddee84eafb8c3da545c88c1d99b94d4"
      val request2 =
        console_api.StoreCredentialRequest(
          mockConnectionId.toString,
          encodedSignedCredential2,
          mockCredentialExternalId.value,
          inclusionProof.encode
        )
      val rpcRequest2 = SignedRpcRequest.generate(keyPair, did, request2)

      usingApiAs(rpcRequest2) { serviceStub =>
        serviceStub.storeCredential(request2)

        val credentials = ReceivedCredentialsDAO
          .getReceivedCredentialsFor(Institution.Id(verifierId.uuid), Some(contactId))
          .transact(database)
          .unsafeToFuture()
          .futureValue

        credentials.size must be(1)

        val credential = credentials.head

        credential.individualId mustBe contactId
        // the credential should be the original one
        credential.encodedSignedCredential mustBe encodedSignedCredential
        credential.externalId mustBe contactExternalId
        credential.merkleInclusionProof mustBe inclusionProof
      }

    }
  }

  "getStoredCredentialsFor" should {
    "get credentials for a specific contact" in {
      val (keyPair, did) = createDid
      testGetStoredCredentialsFor(verifierId, keyPair, did)
    }

    "get credentials for a specific contact using unpublished did auth" in {
      val (keyPair, did) = DIDUtil.createUnpublishedDid
      val verifierUnpubId = DataPreparation.createVerifier()
      testGetStoredCredentialsFor(verifierUnpubId, keyPair, did)
    }

    "get credentials for all contacts" in {
      val (keyPair, did) = createDid
      updateDid(verifierId, did).transact(database).unsafeRunSync()

      val contact1 = DataPreparation.createContact(Institution.Id(verifierId.uuid), "Individual 1", None, "")
      val contactId1 = contact1.contactId
      val contact2 = DataPreparation.createContact(Institution.Id(verifierId.uuid), "Individual 2", None, "")
      val contactId2 = contact2.contactId
      val encodedSignedCredential1 = "1a3cacb2d9e51bdd40264b287db15b4121ddee84eafb8c3da545c88c1d99b94d4"
      val encodedSignedCredential2 = "2a3cacb2d9e51bdd40264b287db15b4121ddee84eafb8c3da545c88c1d99b94d4"
      val connectionId1 = ConnectionId.random()
      val connectionId2 = ConnectionId.random()

      storeCredentialFor(keyPair, did, verifierId, connectionId1, contactId1, encodedSignedCredential1, inclusionProof)
      storeCredentialFor(keyPair, did, verifierId, connectionId2, contactId2, encodedSignedCredential2, inclusionProof)

      val getStoredRequest = console_api.GetStoredCredentialsForRequest()
      val rpcGetStoreRequest = SignedRpcRequest.generate(keyPair, did, getStoredRequest)
      usingApiAs(rpcGetStoreRequest) { serviceStub =>
        val response = serviceStub.getStoredCredentialsFor(getStoredRequest)

        response.credentials.size mustBe 2

        val credentials = response.credentials.map { cred =>
          (cred.individualId, cred.encodedSignedCredential, cred.externalId, cred.batchInclusionProof)
        }

        credentials must contain theSameElementsAs List(
          (contactId1.toString, encodedSignedCredential1, contact1.externalId.value, inclusionProof.encode),
          (contactId2.toString, encodedSignedCredential2, contact2.externalId.value, inclusionProof.encode)
        )
      }
    }
  }

  "getLatestCredentialExternalId" should {
    "return the last id stored" in {
      val (keyPair, did) = createDid
      updateDid(verifierId, did).transact(database).unsafeRunSync()
      testGetLatestCredentialExternalId(verifierId, keyPair, did)
    }

    "return the last id stored using unpublished did auth" in {
      val (keyPair, did) = DIDUtil.createUnpublishedDid
      val verifierUnpubId = DataPreparation.createVerifier()
      testGetLatestCredentialExternalId(verifierUnpubId, keyPair, did)
    }
  }

  private def testStoringCredentialInDB(verifierId: ParticipantId, keyPair: ECKeyPair, did: DID): Assertion = {
    updateDid(verifierId, did).transact(database).unsafeRunSync()

    val contact = DataPreparation.createContact(Institution.Id(verifierId.uuid), "Individual", None, "")
    val contactId = contact.contactId
    val contactExternalId = contact.externalId
    val connectionToken = DataPreparation.generateConnectionToken(Institution.Id(verifierId.uuid), contactId)
    val mockConnectionId = ConnectionId.random()
    ContactsDAO
      .setConnectionAsAccepted(Institution.Id(verifierId.uuid), connectionToken, mockConnectionId)
      .transact(database)
      .unsafeToFuture()
      .futureValue

    val encodedSignedCredential = "a3cacb2d9e51bdd40264b287db15b4121ddee84eafb8c3da545c88c1d99b94d4"
    val mockCredentialExternalId = CredentialExternalId.random()
    val request =
      console_api.StoreCredentialRequest(
        mockConnectionId.toString,
        encodedSignedCredential,
        mockCredentialExternalId.value,
        inclusionProof.encode
      )
    val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

    usingApiAs(rpcRequest) { serviceStub =>
      serviceStub.storeCredential(request)

      val credential = ReceivedCredentialsDAO
        .getReceivedCredentialsFor(Institution.Id(verifierId.uuid), Some(contactId))
        .transact(database)
        .unsafeToFuture()
        .futureValue
        .head

      credential.individualId mustBe contactId
      credential.encodedSignedCredential mustBe encodedSignedCredential
      credential.externalId mustBe contactExternalId
      credential.merkleInclusionProof mustBe inclusionProof
    }
  }

  private def testGetStoredCredentialsFor(verifierId: ParticipantId, keyPair: ECKeyPair, did: DID): Assertion = {
    updateDid(verifierId, did).transact(database).unsafeRunSync()

    val contact1 = DataPreparation.createContact(Institution.Id(verifierId.uuid), "Individual 1", None, "")
    val contactId1 = contact1.contactId
    val contact2 = DataPreparation.createContact(Institution.Id(verifierId.uuid), "Individual 2", None, "")
    val contactId2 = contact2.contactId

    val encodedSignedCredential1 = "1a3cacb2d9e51bdd40264b287db15b4121ddee84eafb8c3da545c88c1d99b94d4"
    val encodedSignedCredential2 = "2a3cacb2d9e51bdd40264b287db15b4121ddee84eafb8c3da545c88c1d99b94d4"
    val connectionId1 = ConnectionId.random()
    val connectionId2 = ConnectionId.random()

    storeCredentialFor(keyPair, did, verifierId, connectionId1, contactId1, encodedSignedCredential1, inclusionProof)
    storeCredentialFor(keyPair, did, verifierId, connectionId2, contactId2, encodedSignedCredential2, inclusionProof)

    val getStoredRequest = console_api.GetStoredCredentialsForRequest(contactId1.toString)
    val rpcGetStoreRequest = SignedRpcRequest.generate(keyPair, did, getStoredRequest)
    usingApiAs(rpcGetStoreRequest) { serviceStub =>
      val response = serviceStub.getStoredCredentialsFor(getStoredRequest)

      response.credentials.size mustBe 1
      val credential = response.credentials.head
      credential.individualId mustBe contactId1.toString
      credential.encodedSignedCredential mustBe encodedSignedCredential1
      credential.externalId mustBe contact1.externalId.value
      credential.batchInclusionProof mustBe inclusionProof.encode
    }
  }

  private def testGetLatestCredentialExternalId(verifierId: ParticipantId, keyPair: ECKeyPair, did: DID): Assertion = {
    updateDid(verifierId, did).transact(database).unsafeRunSync()

    val contactId = DataPreparation.createContact(Institution.Id(verifierId.uuid), "Individual", None, "").contactId

    val connectionToken =
      DataPreparation.generateConnectionToken(Institution.Id(verifierId.uuid), contactId)
    val mockConnectionId = ConnectionId.random()
    ContactsDAO
      .setConnectionAsAccepted(Institution.Id(verifierId.uuid), connectionToken, mockConnectionId)
      .transact(database)
      .unsafeToFuture()
      .futureValue

    // we generate 10 new ids
    val credentialExternalIds = for (_ <- 1 to 10) yield CredentialExternalId.random()
    val encodedSignedCredential = "a3cacb2d9e51bdd40264b287db15b4121ddee84eafb8c3da545c88c1d99b94d4"
    val storeRequests = credentialExternalIds.map { messageId =>
      console_api.StoreCredentialRequest(
        mockConnectionId.toString,
        encodedSignedCredential,
        messageId.value,
        inclusionProof.encode
      )
    }

    storeRequests map { storeRequest =>
      val rpcStoreRequest = SignedRpcRequest.generate(keyPair, did, storeRequest)
      usingApiAs(rpcStoreRequest) { serviceStub =>
        serviceStub.storeCredential(storeRequest)
      }
    }

    val getLastStoredMessageIdRequest = console_api.GetLatestCredentialExternalIdRequest()
    val rpcGetStoreRequest = SignedRpcRequest.generate(keyPair, did, getLastStoredMessageIdRequest)
    usingApiAs(rpcGetStoreRequest) { serviceStub =>
      val response = serviceStub.getLatestCredentialExternalId(getLastStoredMessageIdRequest)

      response.latestCredentialExternalId mustBe credentialExternalIds.last.value
    }
  }
}
