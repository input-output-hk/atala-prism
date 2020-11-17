package io.iohk.atala.mirror

import scala.concurrent.duration._
import scala.util.Try
import monix.eval.Task
import com.google.protobuf.ByteString
import io.grpc.{ManagedChannelBuilder, Metadata}
import io.grpc.stub.{AbstractStub, MetadataUtils}
import org.slf4j.LoggerFactory
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.atala.prism.crypto._
import io.iohk.atala.prism.connector.RequestAuthenticator
import io.iohk.atala.prism.protos.node_models._
import io.iohk.atala.prism.protos.credential_models._
import io.iohk.atala.prism.protos.connector_models.ReceivedMessage
import io.iohk.atala.prism.protos.connector_api._
import io.iohk.atala.prism.protos.node_api.NodeServiceGrpc
import io.iohk.atala.mirror.services._
import io.iohk.atala.mirror.services.BaseGrpcClientService.DidBasedAuthConfig
import io.iohk.atala.mirror.config._
import io.iohk.atala.mirror.db.UserCredentialDao
import io.iohk.atala.mirror.models.{Connection, CredentialProofRequestType, UserCredential}
import monix.execution.Scheduler.Implicits.global
import cats.implicits._
import doobie.implicits._
import io.iohk.atala.prism.identity.DID

class MirrorE2eSpec extends AnyWordSpec with Matchers with PostgresRepositorySpec with MirrorFixtures {

  private val logger = LoggerFactory.getLogger(this.getClass)

  /**
    * This is a E2E test for the Mirror. By default, the test is ignored, because it
    * uses real node and connetor. It simulates mobile app flow, but with signed credential.
    */
  "Mirror" should {
    "generate user credential with validated status" ignore new E2eFixtures {
      (for {
        nodeStub <- Task(createNode("localhost", 50053))
        connectorStub <- Task(createConnector("localhost", 50051))

        // Mirror: create new DID
        did <- Task.fromFuture(connectorStub.registerDID(createDid)).map(response => DID.unsafeFromString(response.did))
        _ = logger.info(s"DID: $did")

        // create services
        connectorConfig = createConnectorConfig(did)

        nodeService = new NodeClientServiceImpl(nodeStub, connectorConfig.authConfig)
        connectorClientService =
          new ConnectorClientServiceImpl(connectorStub, new RequestAuthenticator(EC), connectorConfig)
        credentialService = new CredentialService(
          databaseTask,
          connectorClientService,
          nodeService
        )
        mirrorService = new MirrorService(databaseTask, connectorClientService)

        // Mirror: create new credential
        credential = signedCredential(did)
        credentialResponse <- nodeService.issueCredential(credential.canonicalForm)
        _ = logger.info(s"Credential: ${credentialResponse.id}")

        // Wallet: generate connection token
        connectionToken <- mirrorService.createAccount.map(_.connectionToken)
        _ = logger.info(s"Connection token: $connectionToken")

        // Wallet: create connection from token and add it as header to the connector stub
        connection <-
          connectorClientService
            .authenticatedCall(addConnectionFromTokenRequest(connectionToken), _.addConnectionFromToken)
        _ = logger.info(s"Connection: ${connection}")
        connectionId = connection.connection.map(_.connectionId).getOrElse("")
        connectorClientWithUserId = addHeadersToStub(connectorStub, AuthHeaders.USER_ID -> connection.userId)

        // Mirror: start connection stream to update ConnectionId
        _ <-
          credentialService
            .connectionUpdatesStream(CredentialProofRequestType.SignedCredential(credentialResponse.id))
            .interruptAfter(5.second)
            .compile
            .drain

        // Wallet: get messages for given userId
        proofRequests <-
          Task
            .fromFuture(connectorClientWithUserId.getMessagesPaginated(getMessagesPaginatedRequest))
            .map(_.messages)
        _ = logger.info(s"proofRequests: $proofRequests")

        // Wallet: confirm proof requests
        _ <- Task(proofRequests.flatMap(parseProofRequest(credential.canonicalForm, connectionId)))
          .flatMap(_.map(r => Task.fromFuture(connectorClientWithUserId.sendMessage(r))).toList.sequence)

        // Mirror: process incoming messages
        cardanoAddressService = new CardanoAddressInfoService(databaseTask, mirrorConfig.httpConfig, nodeService)
        connectorMessageService = new ConnectorMessagesService(
          databaseTask,
          connectorClientService,
          List(credentialService.credentialMessageProcessor, cardanoAddressService.cardanoAddressInfoMessageProcessor)
        )
        _ <- connectorMessageService.messagesUpdatesStream.interruptAfter(5.second).compile.drain

        // verify result
        result <-
          UserCredentialDao.findBy(Connection.ConnectionToken(connectionToken)).transact(databaseTask).map(_.head)
      } yield result).runSyncUnsafe(1.minute).status mustBe UserCredential.CredentialStatus.Valid
    }
  }

  trait E2eFixtures {
    val masterKey = EC.generateKeyPair()
    val issuanceKey = EC.generateKeyPair()

    val keyId = "master"

    def signedCredential(did: DID) =
      CredentialFixtures.createSignedCredential(
        CredentialFixtures.createUnsignedCredential(
          keyId,
          did
        ),
        masterKey
      )

    def addConnectionFromTokenRequest(token: String) = AddConnectionFromTokenRequest(token)

    def sendMessageRequest(connectionId: String, message: ByteString) =
      SendMessageRequest(connectionId = connectionId, message = message)

    val getMessagesPaginatedRequest = GetMessagesPaginatedRequest(limit = Int.MaxValue)

    def parseProofRequest(credentialDocument: String, connectionId: String)(message: ReceivedMessage) = {
      for {
        message <- Try(AtalaMessage.parseFrom(message.message.toByteArray)).toOption
        getIssuerSentCredential = message.getIssuerSentCredential
        proofRequest <- message.message.proofRequest
        credential = Credential(typeId = proofRequest.typeIds.head, credentialDocument = credentialDocument)
      } yield sendMessageRequest(connectionId, credential.toByteString)
    }

    val createDid = {
      val createDidOp = CreateDIDOperation(
        didData = Some(
          DIDData(
            publicKeys = Seq(
              PublicKey(
                id = keyId,
                usage = KeyUsage.MASTER_KEY,
                keyData = PublicKey.KeyData.EcKeyData(
                  NodeUtils.toTimestampInfoProto(masterKey.publicKey)
                )
              )
            )
          )
        )
      )

      val atalaOperation = AtalaOperation(operation = AtalaOperation.Operation.CreateDid(createDidOp))

      val signedAtalaOperation = SignedAtalaOperation(
        signedWith = keyId,
        operation = Some(atalaOperation),
        signature = ByteString.copyFrom(EC.sign(atalaOperation.toByteArray, masterKey.privateKey).data)
      )

      RegisterDIDRequest()
        .withCreateDIDOperation(signedAtalaOperation)
        .withLogo(ByteString.EMPTY)
        .withName("mirror")
        .withRole(RegisterDIDRequest.Role.issuer)
    }

    def createConnectorConfig(did: DID) = {
      ConnectorConfig(
        host = "localhost",
        port = 50051,
        authConfig = DidBasedAuthConfig(
          did = did,
          didKeyId = keyId,
          didKeyPair = masterKey
        )
      )
    }

    object AuthHeaders {
      val USER_ID = Metadata.Key.of("userId", Metadata.ASCII_STRING_MARSHALLER)
    }
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

  private def addHeadersToStub[S <: AbstractStub[S]](stub: S, headers: (Metadata.Key[String], String)*): S = {
    val metadata = new Metadata

    headers.foreach {
      case (key, value) => metadata.put(key, value)
    }

    MetadataUtils.attachHeaders(stub, metadata)
  }
}
