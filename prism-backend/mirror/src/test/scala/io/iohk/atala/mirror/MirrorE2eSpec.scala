package io.iohk.atala.mirror

import scala.concurrent.duration._
import monix.eval.Task
import com.google.protobuf.ByteString
import io.grpc.ManagedChannelBuilder
import org.slf4j.LoggerFactory
import org.scalatest.matchers.must.Matchers
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.atala.prism.crypto._
import io.iohk.atala.prism.connector.RequestAuthenticator
import io.iohk.atala.prism.protos.credential_models
import io.iohk.atala.prism.protos.connector_api._
import io.iohk.atala.prism.protos.node_api.NodeServiceGrpc
import io.iohk.atala.mirror.services._
import io.iohk.atala.mirror.db.UserCredentialDao
import io.iohk.atala.mirror.models.UserCredential
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.credentials.{Credential, CredentialBatches}
import io.iohk.atala.prism.credentials.content.CredentialContent
import io.iohk.atala.prism.credentials.content.syntax._
import cats.implicits._
import doobie.implicits._
import io.iohk.atala.prism.daos.ConnectorMessageOffsetDao
import io.iohk.atala.prism.models.{ConnectionToken, CredentialProofRequestType}
import monix.execution.Scheduler.Implicits.global
import io.iohk.atala.prism.services.BaseGrpcClientService.PublicKeyBasedAuthConfig
import io.iohk.atala.prism.services.{
  BaseGrpcClientService,
  ConnectorClientServiceImpl,
  ConnectorMessagesService,
  NodeClientServiceImpl
}
import io.iohk.atala.prism.E2ETestUtils._

// sbt "project mirror" "testOnly *MirrorE2eSpec"
class MirrorE2eSpec extends PostgresRepositorySpec[Task] with Matchers with MirrorFixtures {

  implicit val ecTrait = EC

  private val logger = LoggerFactory.getLogger(this.getClass)

  /**
    * This is a E2E test for the Mirror. By default, the test is ignored, because it
    * uses real node and connector. It simulates mobile app flow, but with signed credential.
    */
  "Mirror" should {
    "generate user credential with validated status" ignore new E2eFixtures {
      (for {
        nodeStub <- Task(createNode("localhost", 50053))
        connectorStub <- Task(createConnector("localhost", 50051))

        // Mirror: create new DID
        did <-
          Task
            .fromFuture(connectorStub.registerDID(createDid(masterKey, keyId, masterKey)))
            .map(response => DID.unsafeFromString(response.did))
        _ = logger.info(s"DID: ${did.value}")

        // create services
        authConfig = createAuthConfig(did, masterKey, "master", issuanceKey, "issuance")

        nodeService = new NodeClientServiceImpl(nodeStub, authConfig)
        connectorClientService =
          new ConnectorClientServiceImpl(connectorStub, new RequestAuthenticator(ecTrait), authConfig)
        credentialService = new CredentialService(
          database,
          connectorClientService,
          nodeService
        )
        mirrorService = new MirrorServiceImpl(database, connectorClientService)

        // Wallet: connector with public key auth
        walletConnectorClientService = new BaseGrpcClientService(
          connectorStub,
          new RequestAuthenticator(ecTrait),
          PublicKeyBasedAuthConfig(walletKey)
        ) {}

        // Mirror: create a new credential and a credential batch
        credential = signedCredential(did)
        (root, proof :: _) = CredentialBatches.batch(List(credential))
        credentialResponse <- nodeService.issueCredentialBatch(root)
        _ = logger.info(s"Credential batch issued, batchId: ${credentialResponse.batchId}")

        credentialMessage = credential_models.PlainTextCredential(
          encodedCredential = credential.canonicalForm,
          encodedMerkleProof = proof.encode
        )

        // Wallet: generate connection token
        connectionToken <- mirrorService.createAccount.map(_.connectionToken)
        _ = logger.info(s"Connection token: $connectionToken")

        // Wallet: create connection from token and add it as header to the connector stub
        connection <-
          walletConnectorClientService
            .authenticatedCall(addConnectionFromTokenRequest(connectionToken, walletKey), _.addConnectionFromToken)
        _ = logger.info(s"Connection: $connection")
        connectionId = connection.connection.map(_.connectionId).getOrElse("")

        // Mirror: start connection stream to update ConnectionId
        _ <-
          credentialService
            .connectionUpdatesStream(CredentialProofRequestType.RedlandIdCredential)
            .interruptAfter(5.second)
            .compile
            .drain

        // Wallet: get messages for given userId
        proofRequests <-
          walletConnectorClientService
            .authenticatedCall(getMessagesPaginatedRequest, _.getMessagesPaginated)
            .map(_.messages)
        _ = logger.info(s"proofRequests: $proofRequests")

        // Wallet: confirm proof requests
        _ <- walletConnectorClientService.authenticatedCall(
          sendMessageRequest(connectionId, credentialMessage.toByteString),
          _.sendMessage
        )

        // Mirror: process incoming messages
        cardanoAddressService = new CardanoAddressInfoService(database, mirrorConfig.httpConfig, nodeService)
        connectorMessageService = new ConnectorMessagesService(
          connectorService = connectorClientService,
          List(credentialService.credentialMessageProcessor, cardanoAddressService.cardanoAddressInfoMessageProcessor),
          findLastMessageOffset = ConnectorMessageOffsetDao.findLastMessageOffset().transact(database),
          saveMessageOffset = ConnectorMessageOffsetDao.updateLastMessageOffset(_).transact(database).void
        )
        _ <- connectorMessageService.messagesUpdatesStream.interruptAfter(5.second).compile.drain

        // verify result
        result <- UserCredentialDao.findBy(ConnectionToken(connectionToken)).transact(database).map(_.head)
      } yield result).runSyncUnsafe(1.minute).status mustBe UserCredential.CredentialStatus.Valid
    }
  }

  trait E2eFixtures {
    val walletKey = ecTrait.generateKeyPair()
    val masterKey = ecTrait.generateKeyPair()
    val issuanceKey = ecTrait.generateKeyPair()

    val keyId = "master"

    def signedCredential(did: DID) = {
      val credentialContent = CredentialContent(
        CredentialContent.JsonFields.IssuerDid.field -> did.value,
        CredentialContent.JsonFields.IssuanceKeyId.field -> keyId
      )

      Credential
        .fromCredentialContent(credentialContent)
        .sign(masterKey.privateKey)
    }

    def sendMessageRequest(connectionId: String, message: ByteString) =
      SendMessageRequest(connectionId = connectionId, message = message)

    val getMessagesPaginatedRequest = GetMessagesPaginatedRequest(limit = Int.MaxValue)
  }

  def createNode(
      host: String,
      port: Int
  ): NodeServiceGrpc.NodeServiceStub = {
    val channel = ManagedChannelBuilder
      .forAddress(host, port)
      .usePlaintext()
      .build()

    NodeServiceGrpc.stub(channel)
  }

  private def createConnector(
      host: String,
      port: Int
  ): ConnectorServiceGrpc.ConnectorServiceStub = {
    val channel = ManagedChannelBuilder
      .forAddress(host, port)
      .usePlaintext()
      .build()

    ConnectorServiceGrpc.stub(channel)
  }
}
