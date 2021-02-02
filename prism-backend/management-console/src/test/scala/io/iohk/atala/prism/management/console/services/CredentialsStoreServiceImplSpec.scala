package io.iohk.atala.prism.management.console.services

import doobie.implicits._
import io.iohk.atala.prism.auth.SignedRpcRequest
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeaderParser
import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.management.console.models.{CredentialExternalId, ParticipantId, ParticipantLogo}
import io.iohk.atala.prism.management.console.repositories.ParticipantsRepository.CreateParticipantRequest
import io.iohk.atala.prism.management.console.repositories.daos._
import io.iohk.atala.prism.management.console.repositories.{
  ParticipantsRepository,
  ReceivedCredentialsRepository,
  RequestNoncesRepository
}
import io.iohk.atala.prism.management.console.{DataPreparation, ManagementConsoleAuthenticator}
import io.iohk.atala.prism.protos.console_api
import io.iohk.atala.prism.{DIDGenerator, RpcSpecBase}
import org.mockito.MockitoSugar._

class CredentialsStoreServiceImplSpec extends RpcSpecBase with DIDGenerator {
  val usingApiAs = usingApiAsConstructor(
    new console_api.CredentialsStoreServiceGrpc.CredentialsStoreServiceBlockingStub(_, _)
  )

  lazy val receivedCredentials = new ReceivedCredentialsRepository(database)
  private lazy val participantsRepository = new ParticipantsRepository(database)(executionContext)
  private lazy val requestNoncesRepository = new RequestNoncesRepository.PostgresImpl(database)(executionContext)
  protected lazy val nodeMock = mock[io.iohk.atala.prism.protos.node_api.NodeServiceGrpc.NodeService]

  private lazy val authenticator = new ManagementConsoleAuthenticator(
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
          new CredentialsStoreServiceImpl(receivedCredentials, authenticator),
          executionContext
        )
    )

  override def beforeEach(): Unit = {
    super.beforeEach()

    new ParticipantsRepository(database)
      .create(
        CreateParticipantRequest(
          verifierId,
          "Verifier",
          DataPreparation.newDID(),
          ParticipantLogo(Vector())
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
         |WHERE participant_id = $participantId
       """.stripMargin.update.run.map(_ => ())
  }

  "storeCredential" should {
    "store credential in the database" in {
      lazy val keyPair = EC.generateKeyPair()
      lazy val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      updateDid(verifierId, did).transact(database).unsafeRunSync()

      val contactId = DataPreparation.createContact(verifierId, "Individual", None).contactId

      val encodedSignedCredential = "a3cacb2d9e51bdd40264b287db15b4121ddee84eafb8c3da545c88c1d99b94d4"
      val mockCredentialExternalId = CredentialExternalId.random()
      val request =
        console_api.StoreCredentialRequest(
          contactId.toString,
          encodedSignedCredential,
          mockCredentialExternalId.value
        )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        serviceStub.storeCredential(request)

        val credential = ReceivedCredentialsDAO
          .getReceivedCredentialsFor(verifierId, contactId)
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

      val contactId = DataPreparation.createContact(verifierId, "Individual", None).contactId

      val encodedSignedCredential = "a3cacb2d9e51bdd40264b287db15b4121ddee84eafb8c3da545c88c1d99b94d4"
      val mockCredentialExternalId = CredentialExternalId.random()
      val request =
        console_api.StoreCredentialRequest(
          contactId.toString,
          encodedSignedCredential,
          mockCredentialExternalId.value
        )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        serviceStub.storeCredential(request)

        val credential = ReceivedCredentialsDAO
          .getReceivedCredentialsFor(verifierId, contactId)
          .transact(database)
          .unsafeToFuture()
          .futureValue
          .head

        credential.individualId mustBe contactId
        credential.encodedSignedCredential mustBe encodedSignedCredential
      }

      val encodedSignedCredential2 = "b3cacb2d9e51bdd40264b287db15b4121ddee84eafb8c3da545c88c1d99b94d4"
      val request2 =
        console_api.StoreCredentialRequest(
          contactId.toString,
          encodedSignedCredential2,
          mockCredentialExternalId.value
        )
      val rpcRequest2 = SignedRpcRequest.generate(keyPair, did, request2)

      usingApiAs(rpcRequest2) { serviceStub =>
        serviceStub.storeCredential(request2)

        val credentials = ReceivedCredentialsDAO
          .getReceivedCredentialsFor(verifierId, contactId)
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

  "getCredentialsFor" should {
    "get credentials for individual" in {
      lazy val keyPair = EC.generateKeyPair()
      lazy val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      updateDid(verifierId, did).transact(database).unsafeRunSync()

      val contactId = DataPreparation.createContact(verifierId, "Individual", None).contactId

      val encodedSignedCredential = "a3cacb2d9e51bdd40264b287db15b4121ddee84eafb8c3da545c88c1d99b94d4"
      val mockCredentialExternalId = CredentialExternalId.random()
      val storeRequest = console_api.StoreCredentialRequest(
        contactId.toString,
        encodedSignedCredential,
        mockCredentialExternalId.value
      )
      val rpcStoreRequest = SignedRpcRequest.generate(keyPair, did, storeRequest)
      usingApiAs(rpcStoreRequest) { serviceStub =>
        serviceStub.storeCredential(storeRequest)
      }

      val getStoredRequest = console_api.GetStoredCredentialsForRequest(contactId.toString)
      val rpcGetStoreRequest = SignedRpcRequest.generate(keyPair, did, getStoredRequest)
      usingApiAs(rpcGetStoreRequest) { serviceStub =>
        val response = serviceStub.getStoredCredentialsFor(getStoredRequest)

        response.credentials.size mustBe 1
        val credential = response.credentials.head
        credential.individualId mustBe contactId.toString
        credential.encodedSignedCredential mustBe encodedSignedCredential
      }
    }
  }

  "getLatestCredentialExternalId" should {
    "return the last id stored" in {
      lazy val keyPair = EC.generateKeyPair()
      lazy val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      updateDid(verifierId, did).transact(database).unsafeRunSync()

      val contactId = DataPreparation.createContact(verifierId, "Individual", None).contactId

      // we generate 10 new ids
      val credentialExternalIds = for (_ <- 1 to 10) yield CredentialExternalId.random()
      val encodedSignedCredential = "a3cacb2d9e51bdd40264b287db15b4121ddee84eafb8c3da545c88c1d99b94d4"
      val storeRequests = credentialExternalIds.map { messageId =>
        console_api.StoreCredentialRequest(
          contactId.toString,
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

      val getLastStoredMessageIdRequest = console_api.GetLatestCredentialExternalIdRequest()
      val rpcGetStoreRequest = SignedRpcRequest.generate(keyPair, did, getLastStoredMessageIdRequest)
      usingApiAs(rpcGetStoreRequest) { serviceStub =>
        val response = serviceStub.getLatestCredentialExternalId(getLastStoredMessageIdRequest)

        response.latestCredentialExternalId mustBe credentialExternalIds.last.value
      }
    }
  }
}
