package io.iohk.atala.prism.management.console.services

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.syntax.functor._
import doobie.implicits._
import io.iohk.atala.prism.auth.{AuthenticatorF, SignedRpcRequest}
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.management.console.grpc.CredentialsStoreGrpcService
import io.iohk.atala.prism.management.console.models.{CredentialExternalId, ParticipantId, ParticipantLogo}
import io.iohk.atala.prism.management.console.repositories.ParticipantsRepository.CreateParticipantRequest
import io.iohk.atala.prism.management.console.repositories.daos._
import io.iohk.atala.prism.management.console.repositories.{
  ContactsRepository,
  ParticipantsRepository,
  ReceivedCredentialsRepository,
  RequestNoncesRepository
}
import io.iohk.atala.prism.management.console.{DataPreparation, ManagementConsoleAuthenticator}
import io.iohk.atala.prism.protos.console_api
import io.iohk.atala.prism.utils.IOUtils._
import io.iohk.atala.prism.{DIDUtil, RpcSpecBase}
import org.mockito.MockitoSugar._
import tofu.logging.Logs

class CredentialsStoreServiceImplSpec extends RpcSpecBase with DIDUtil {

  private val managementConsoleTestLogs: Logs[IO, IOWithTraceIdContext] =
    Logs.withContext[IO, IOWithTraceIdContext]

  val usingApiAs = usingApiAsConstructor(
    new console_api.CredentialsStoreServiceGrpc.CredentialsStoreServiceBlockingStub(
      _,
      _
    )
  )

  private lazy val receivedCredentialsRepository =
    ReceivedCredentialsRepository.unsafe(
      dbLiftedToTraceIdIO,
      managementConsoleTestLogs
    )
  private lazy val contactsRepository =
    ContactsRepository.unsafe(dbLiftedToTraceIdIO, managementConsoleTestLogs)
  private lazy val participantsRepository =
    ParticipantsRepository.unsafe(
      dbLiftedToTraceIdIO,
      managementConsoleTestLogs
    )
  private lazy val requestNoncesRepository =
    RequestNoncesRepository.unsafe(
      dbLiftedToTraceIdIO,
      managementConsoleTestLogs
    )
  protected lazy val nodeMock =
    mock[io.iohk.atala.prism.protos.node_api.NodeServiceGrpc.NodeService]

  private lazy val authenticator = AuthenticatorF.unsafe(
    nodeMock,
    new ManagementConsoleAuthenticator(
      participantsRepository,
      requestNoncesRepository
    ),
    managementConsoleTestLogs
  )
  lazy val verifierId =
    ParticipantId.unsafeFrom("af45a4da-65b8-473e-aadc-aa6b346250a3")

  override def services =
    Seq(
      console_api.CredentialsStoreServiceGrpc
        .bindService(
          new CredentialsStoreGrpcService(
            CredentialsStoreService
              .unsafe(receivedCredentialsRepository, managementConsoleTestLogs),
            contactsRepository,
            authenticator
          ),
          executionContext
        )
    )

  override def beforeEach(): Unit = {
    super.beforeEach()

    participantsRepository
      .create(
        CreateParticipantRequest(
          verifierId,
          "Verifier",
          DataPreparation.newDID(),
          ParticipantLogo(Vector())
        )
      )
      .run(TraceId.generateYOLO)
      .unsafeRunSync()
    ()
  }

  def updateDid(
      participantId: ParticipantId,
      did: DID
  ): doobie.ConnectionIO[Unit] = {
    sql"""
         |UPDATE participants
         |SET did = $did
         |WHERE participant_id = $participantId
       """.stripMargin.update.run.void
  }
  val mockMerkleInclusionProof =
    """{"hash":"7d25e48be1c6475429bd33adbd5b7657340f264e62c2bf9b25ea478d9d3a2566","index":0,"siblings":[]}"""

  "storeCredential" should {
    "store credential in the database" in {
      lazy val keyPair = EC.generateKeyPair()
      lazy val publicKey = keyPair.getPublicKey
      val did = generateDid(publicKey)
      updateDid(verifierId, did).transact(database).unsafeRunSync()

      val token = "token" + publicKey.getHexEncoded
      val contactId = DataPreparation
        .createContact(verifierId, "Individual", None, connectionToken = token)
        .contactId

      val encodedSignedCredential =
        "a3cacb2d9e51bdd40264b287db15b4121ddee84eafb8c3da545c88c1d99b94d4"
      val mockCredentialExternalId = CredentialExternalId.random()
      val request = console_api
        .StoreCredentialRequest()
        .withConnectionToken(token)
        .withEncodedSignedCredential(encodedSignedCredential)
        .withCredentialExternalId(mockCredentialExternalId.value)
        .withBatchInclusionProof(mockMerkleInclusionProof)
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        serviceStub.storeCredential(request)

        val credential = ReceivedCredentialsDAO
          .getReceivedCredentialsFor(verifierId, Some(contactId))
          .transact(database)
          .unsafeRunSync()
          .head

        credential.individualId mustBe contactId
        credential.encodedSignedCredential mustBe encodedSignedCredential
      }
    }

    "NOT store credential in the database when there is a conflict by external id" in {
      lazy val keyPair = EC.generateKeyPair()
      lazy val publicKey = keyPair.getPublicKey
      val did = generateDid(publicKey)
      updateDid(verifierId, did).transact(database).unsafeRunSync()

      val token = "token" + publicKey.getHexEncoded
      val contactId = DataPreparation
        .createContact(verifierId, "Individual", None, connectionToken = token)
        .contactId

      val encodedSignedCredential =
        "a3cacb2d9e51bdd40264b287db15b4121ddee84eafb8c3da545c88c1d99b94d4"
      val mockCredentialExternalId = CredentialExternalId.random()
      val request = console_api
        .StoreCredentialRequest()
        .withConnectionToken(token)
        .withEncodedSignedCredential(encodedSignedCredential)
        .withCredentialExternalId(mockCredentialExternalId.value)
        .withBatchInclusionProof(mockMerkleInclusionProof)
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        serviceStub.storeCredential(request)

        val credential = ReceivedCredentialsDAO
          .getReceivedCredentialsFor(verifierId, Some(contactId))
          .transact(database)
          .unsafeRunSync()
          .head

        credential.individualId mustBe contactId
        credential.encodedSignedCredential mustBe encodedSignedCredential
      }

      val encodedSignedCredential2 =
        "b3cacb2d9e51bdd40264b287db15b4121ddee84eafb8c3da545c88c1d99b94d4"

      val request2 = console_api
        .StoreCredentialRequest()
        .withConnectionToken(token)
        .withEncodedSignedCredential(encodedSignedCredential2)
        .withCredentialExternalId(mockCredentialExternalId.value)
        .withBatchInclusionProof(mockMerkleInclusionProof)
      val rpcRequest2 = SignedRpcRequest.generate(keyPair, did, request2)

      usingApiAs(rpcRequest2) { serviceStub =>
        serviceStub.storeCredential(request2)

        val credentials = ReceivedCredentialsDAO
          .getReceivedCredentialsFor(verifierId, Some(contactId))
          .transact(database)
          .unsafeRunSync()

        credentials.size must be(1)

        val credential = credentials.head

        credential.individualId mustBe contactId
        // the credential should be the original one
        credential.encodedSignedCredential mustBe encodedSignedCredential
      }
    }

    "fail when the connection token does not belong to the authenticated institution" in {
      lazy val keyPair = EC.generateKeyPair()
      lazy val publicKey = keyPair.getPublicKey
      val did = generateDid(publicKey)
      updateDid(verifierId, did).transact(database).unsafeRunSync()

      val verifier2 = ParticipantId.random()
      participantsRepository
        .create(
          CreateParticipantRequest(
            verifier2,
            "Verifier 2",
            DataPreparation.newDID(),
            ParticipantLogo(Vector())
          )
        )
        .run(TraceId.generateYOLO)
        .unsafeRunSync()

      // the contact belongs to verifier2
      val token = "token" + publicKey.getHexEncoded
      DataPreparation
        .createContact(verifier2, "Individual", None, connectionToken = token)
        .contactId

      // still, another verifier tries to push a credential to verifier2, which fails
      val encodedSignedCredential =
        "a3cacb2d9e51bdd40264b287db15b4121ddee84eafb8c3da545c88c1d99b94d4"
      val mockCredentialExternalId = CredentialExternalId.random()
      val request = console_api
        .StoreCredentialRequest()
        .withConnectionToken(token)
        .withEncodedSignedCredential(encodedSignedCredential)
        .withCredentialExternalId(mockCredentialExternalId.value)
        .withBatchInclusionProof(mockMerkleInclusionProof)
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        val ex = intercept[io.grpc.StatusRuntimeException] {
          serviceStub.storeCredential(request)
        }

        ex.getMessage.contains("Unknown token") must be(true)
      }
    }
  }

  "getCredentialsFor" should {
    "get all credentials when the individual argument is missing" in {
      lazy val keyPair = EC.generateKeyPair()
      lazy val publicKey = keyPair.getPublicKey
      val did = generateDid(publicKey)
      updateDid(verifierId, did).transact(database).unsafeRunSync()

      val token = "token" + publicKey.getHexEncoded
      val contactId = DataPreparation
        .createContact(verifierId, "Individual", None, connectionToken = token)
        .contactId

      val encodedSignedCredential =
        "a3cacb2d9e51bdd40264b287db15b4121ddee84eafb8c3da545c88c1d99b94d4"
      val mockCredentialExternalId = CredentialExternalId.random()
      val storeRequest = console_api
        .StoreCredentialRequest()
        .withConnectionToken(token)
        .withEncodedSignedCredential(encodedSignedCredential)
        .withCredentialExternalId(mockCredentialExternalId.value)
        .withBatchInclusionProof(mockMerkleInclusionProof)

      val rpcStoreRequest =
        SignedRpcRequest.generate(keyPair, did, storeRequest)
      usingApiAs(rpcStoreRequest) { serviceStub =>
        serviceStub.storeCredential(storeRequest)
      }

      val getStoredRequest = console_api.GetStoredCredentialsForRequest()
      val rpcGetStoreRequest =
        SignedRpcRequest.generate(keyPair, did, getStoredRequest)
      usingApiAs(rpcGetStoreRequest) { serviceStub =>
        val response = serviceStub.getStoredCredentialsFor(getStoredRequest)

        response.credentials.size mustBe 1
        val credential = response.credentials.head
        credential.individualId mustBe contactId.toString
        credential.encodedSignedCredential mustBe encodedSignedCredential
      }
    }

    "get credentials for individual" in {
      lazy val keyPair = EC.generateKeyPair()
      lazy val publicKey = keyPair.getPublicKey
      val did = generateDid(publicKey)
      updateDid(verifierId, did).transact(database).unsafeRunSync()

      val token = "token" + publicKey.getHexEncoded
      val contactId = DataPreparation
        .createContact(verifierId, "Individual", None, connectionToken = token)
        .contactId

      val encodedSignedCredential =
        "a3cacb2d9e51bdd40264b287db15b4121ddee84eafb8c3da545c88c1d99b94d4"
      val mockCredentialExternalId = CredentialExternalId.random()
      val storeRequest = console_api
        .StoreCredentialRequest()
        .withConnectionToken(token)
        .withEncodedSignedCredential(encodedSignedCredential)
        .withCredentialExternalId(mockCredentialExternalId.value)
        .withBatchInclusionProof(mockMerkleInclusionProof)
      val rpcStoreRequest =
        SignedRpcRequest.generate(keyPair, did, storeRequest)
      usingApiAs(rpcStoreRequest) { serviceStub =>
        serviceStub.storeCredential(storeRequest)
      }

      val getStoredRequest =
        console_api.GetStoredCredentialsForRequest(contactId.toString)
      val rpcGetStoreRequest =
        SignedRpcRequest.generate(keyPair, did, getStoredRequest)
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
      lazy val publicKey = keyPair.getPublicKey
      val did = generateDid(publicKey)
      updateDid(verifierId, did).transact(database).unsafeRunSync()

      val token = "token" + publicKey.getHexEncoded
      DataPreparation
        .createContact(verifierId, "Individual", None, connectionToken = token)
        .contactId

      // we generate 10 new ids
      val credentialExternalIds =
        for (_ <- 1 to 10) yield CredentialExternalId.random()
      val encodedSignedCredential =
        "a3cacb2d9e51bdd40264b287db15b4121ddee84eafb8c3da545c88c1d99b94d4"
      val storeRequests = credentialExternalIds.map { messageId =>
        console_api
          .StoreCredentialRequest()
          .withConnectionToken(token)
          .withEncodedSignedCredential(encodedSignedCredential)
          .withCredentialExternalId(messageId.value)
          .withBatchInclusionProof(mockMerkleInclusionProof)
      }

      storeRequests.map { storeRequest =>
        val rpcStoreRequest =
          SignedRpcRequest.generate(keyPair, did, storeRequest)
        usingApiAs(rpcStoreRequest) { serviceStub =>
          serviceStub.storeCredential(storeRequest)
        }
      }

      val getLastStoredMessageIdRequest =
        console_api.GetLatestCredentialExternalIdRequest()
      val rpcGetStoreRequest =
        SignedRpcRequest.generate(keyPair, did, getLastStoredMessageIdRequest)
      usingApiAs(rpcGetStoreRequest) { serviceStub =>
        val response = serviceStub.getLatestCredentialExternalId(
          getLastStoredMessageIdRequest
        )

        response.latestCredentialExternalId mustBe credentialExternalIds.last.value
      }
    }
  }
}
