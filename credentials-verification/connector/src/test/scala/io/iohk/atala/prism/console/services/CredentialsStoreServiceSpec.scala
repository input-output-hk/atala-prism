package io.iohk.atala.prism.console.services

import java.util.UUID

import doobie.implicits._
import io.iohk.atala.prism.{DIDGenerator, RpcSpecBase}
import io.iohk.atala.prism.auth.SignedRpcRequest
import io.iohk.atala.prism.connector.model.{ConnectionId, ParticipantLogo, ParticipantType}
import io.iohk.atala.prism.connector.repositories.ParticipantsRepository.CreateParticipantRequest
import io.iohk.atala.prism.connector.repositories.daos._
import io.iohk.atala.prism.connector.repositories.{ParticipantsRepository, RequestNoncesRepository}
import io.iohk.atala.prism.connector.ConnectorAuthenticator
import io.iohk.atala.prism.console.DataPreparation
import io.iohk.atala.prism.console.models.Institution
import io.iohk.atala.prism.console.repositories.StoredCredentialsRepository
import io.iohk.atala.prism.console.repositories.daos.{ContactsDAO, StoredCredentialsDAO}
import io.iohk.atala.prism.crypto.{EC, SHA256Digest}
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeaderParser
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.models.{Ledger, ParticipantId, TransactionId, TransactionInfo}
import io.iohk.atala.prism.protos.cstore_api
import org.mockito.MockitoSugar._
import org.scalatest.OptionValues._

import scala.concurrent.duration._

class CredentialsStoreServiceSpec extends RpcSpecBase with DIDGenerator {

  implicit val executionContext = scala.concurrent.ExecutionContext.global

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 20.millis)

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
      val request =
        cstore_api.StoreCredentialRequest(mockConnectionId.id.toString, encodedSignedCredential)
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        serviceStub.storeCredential(request)

        val credential = StoredCredentialsDAO
          .getStoredCredentialsFor(Institution.Id(verifierId.uuid), contactId)
          .transact(database)
          .unsafeToFuture()
          .futureValue
          .head

        credential.individualId mustBe contactId
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

      val contactId = DataPreparation.createContact(Institution.Id(verifierId.uuid), "Individual", None, "").contactId

      val connectionToken =
        DataPreparation.generateConnectionToken(Institution.Id(verifierId.uuid), contactId)
      val mockConnectionId = ConnectionId(UUID.randomUUID())
      ContactsDAO
        .setConnectionAsAccepted(Institution.Id(verifierId.uuid), connectionToken, mockConnectionId)
        .transact(database)
        .unsafeToFuture()
        .futureValue

      val encodedSignedCredential = "a3cacb2d9e51bdd40264b287db15b4121ddee84eafb8c3da545c88c1d99b94d4"
      val storeRequest = cstore_api.StoreCredentialRequest(mockConnectionId.id.toString, encodedSignedCredential)
      val rpcStoreRequest = SignedRpcRequest.generate(keyPair, did, storeRequest)
      usingApiAs(rpcStoreRequest) { serviceStub =>
        serviceStub.storeCredential(storeRequest)
      }

      val getStoredRequest = cstore_api.GetStoredCredentialsForRequest(contactId.value.toString)
      val rpcGetStoreRequest = SignedRpcRequest.generate(keyPair, did, getStoredRequest)
      usingApiAs(rpcGetStoreRequest) { serviceStub =>
        val response = serviceStub.getStoredCredentialsFor(getStoredRequest)

        response.credentials.size mustBe 1
        val credential = response.credentials.head
        credential.individualId mustBe contactId.value.toString
        credential.encodedSignedCredential mustBe encodedSignedCredential
      }
    }
  }
}
