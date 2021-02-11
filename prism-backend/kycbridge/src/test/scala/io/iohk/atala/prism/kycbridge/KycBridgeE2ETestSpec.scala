package io.iohk.atala.prism.kycbridge

import com.google.protobuf.ByteString
import io.grpc.ManagedChannelBuilder
import io.iohk.atala.kycbridge.protos.kycbridge_api.KycBridgeServiceGrpc
import io.iohk.atala.kycbridge.protos.kycbridge_api.CreateAccountRequest
import io.iohk.atala.prism.E2ETestUtils._
import io.iohk.atala.prism.protos.credential_models.{
  AcuantProcessFinished,
  AtalaMessage,
  KycBridgeMessage,
  PlainTextCredential
}
import io.iohk.atala.prism.connector.RequestAuthenticator
import io.iohk.atala.prism.crypto.{EC, ECKeyPair}
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.protos.connector_api.{
  ConnectorServiceGrpc,
  GetMessagesPaginatedRequest,
  SendMessageRequest,
  SendMessageResponse
}
import io.iohk.atala.prism.protos.connector_models.ReceivedMessage
import io.iohk.atala.prism.services.BaseGrpcClientService.PublicKeyBasedAuthConfig
import io.iohk.atala.prism.services.BaseGrpcClientService
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import monix.execution.Scheduler.Implicits.global
import org.http4s.client.Client
import org.slf4j.LoggerFactory
import io.iohk.atala.prism.kycbridge.services.ServiceUtils.runRequestToEither
import org.http4s.{AuthScheme, Credentials, Uri}
import org.http4s.Method.POST
import org.http4s.headers.Authorization
import monix.eval.Task
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.dsl.Http4sClientDsl
import io.iohk.atala.prism.utils.GrpcUtils

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

// Before running this E2E test make sure that all necessary services (node, connector and kycBridge)
// are running. Check README.md for instructions.
// sbt "project kycbridge" "testOnly *KycBridgeE2ETestSpec"
class KycBridgeE2ETestSpec extends AnyWordSpec with Matchers with KycBridgeFixtures with Http4sClientDsl[Task] {

  private val FETCH_CONNECTOR_MESSAGE_MAX_RETRIES = 20
  private val FETCH_CONNECTOR_MESSAGE_WAIT_TIME = 5.seconds

  implicit val ecTrait = EC

  private val logger = LoggerFactory.getLogger(this.getClass)

  "KycBridge" should {
    "issue credential based on Acuant data" ignore new E2EFixtures {

      val kycBridgeStub = createKycBridgeStub("localhost", 50050)
      val connectorStub = GrpcUtils.createPlaintextStub("localhost", 50051, ConnectorServiceGrpc.stub)

      (for {
        httpClientAllocatedResource <- BlazeClientBuilder[Task](ExecutionContext.Implicits.global).resource.allocated

        (httpClient, releaseHttpClient) = httpClientAllocatedResource

        baseGrpcClientService = new BaseGrpcClientService(
          connectorStub,
          new RequestAuthenticator(ecTrait),
          PublicKeyBasedAuthConfig(clientKey)
        ) {}

        did <-
          Task
            .fromFuture(connectorStub.registerDID(createDid(clientKey, keyId, clientKey)))
            .map(response => DID.unsafeFromString(response.did))

        _ = logger.info(s"DID created: ${did.value}")

        createAccountResponse <- Task.fromFuture(kycBridgeStub.createAccount(CreateAccountRequest()))

        _ = logger.info(s"Connection token for client created: ${createAccountResponse.connectionToken}")

        connection <-
          baseGrpcClientService
            .authenticatedCall(
              addConnectionFromTokenRequest(createAccountResponse.connectionToken, clientKey),
              _.addConnectionFromToken
            )

        _ = logger.info(s"Connection initiated: $connection")

        connectionId <-
          connection.connection
            .map(_.connectionId)
            .map(Task.pure)
            .getOrElse(Task.raiseError(new Throwable("connectionId is missing")))

        _ = logger.info("Waiting for StartAcuantProcess message")

        receivedMessage <- fetchConnectorMessage(baseGrpcClientService)

        startAcuantProcessMessage =
          AtalaMessage.parseFrom(receivedMessage.message.toByteArray).getKycBridgeMessage.getStartAcuantProcess

        _ = logger.info(
          s"StartAcuantProcess message obtained: bearer token: ${startAcuantProcessMessage.bearerToken} " +
            s"instanceId: ${startAcuantProcessMessage.documentInstanceId} "
        )

        _ = logger.info("Sending document photo to Acuant")

        _ <- sendDocumentPhotoToAcuant(
          startAcuantProcessMessage.documentInstanceId,
          startAcuantProcessMessage.bearerToken,
          httpClient
        )

        _ = logger.info("Document photo send")

        _ <- sendAcuantProcessFinishedMessage(
          baseGrpcClientService,
          connectionId,
          startAcuantProcessMessage.documentInstanceId
        )

        _ = logger.info("AcuantProcessFinished send, waiting for response from KycBridge")

        credentialMessage <- fetchConnectorMessage(baseGrpcClientService, lastSeenMessageId = Some(receivedMessage.id))

        credential = PlainTextCredential.parseFrom(credentialMessage.message.toByteArray)

        _ = logger.info(s"Credential successfully obtained from kyc bridge: ${credential.encodedCredential}")

        _ <- releaseHttpClient
      } yield ()).runSyncUnsafe()
    }
  }

  def createKycBridgeStub(
      host: String,
      port: Int
  ): KycBridgeServiceGrpc.KycBridgeServiceStub = {
    val channel = ManagedChannelBuilder
      .forAddress(host, port)
      .usePlaintext()
      .build()

    KycBridgeServiceGrpc.stub(channel)
  }

  def fetchConnectorMessage(
      baseGrpcClientService: BaseGrpcClientService[ConnectorServiceGrpc.ConnectorServiceStub],
      attemptNumber: Int = 1,
      lastSeenMessageId: Option[String] = None
  ): Task[ReceivedMessage] = {
    if (attemptNumber >= FETCH_CONNECTOR_MESSAGE_MAX_RETRIES)
      Task.raiseError(new Throwable("Cannot fetch message from connector"))
    else {
      logger.info(s"Trying to fetch message from connector attempt: $attemptNumber")
      baseGrpcClientService
        .authenticatedCall(GetMessagesPaginatedRequest(lastSeenMessageId.getOrElse(""), 10), _.getMessagesPaginated)
        .flatMap(response =>
          response.messages.toList match {
            case message :: _ => Task.pure(message)
            case _ =>
              Task
                .sleep(FETCH_CONNECTOR_MESSAGE_WAIT_TIME)
                .flatMap(_ => fetchConnectorMessage(baseGrpcClientService, attemptNumber + 1, lastSeenMessageId))
          }
        )
    }
  }

  def sendAcuantProcessFinishedMessage(
      baseGrpcClientService: BaseGrpcClientService[ConnectorServiceGrpc.ConnectorServiceStub],
      connectionId: String,
      documentInstanceId: String
  ): Task[SendMessageResponse] = {
    val selfiePhoto = readFileFromResource("selfiePhoto.jpg")

    val acuantProcessFinished = AcuantProcessFinished(documentInstanceId, ByteString.copyFrom(selfiePhoto))
    val kycBridgeMessage = KycBridgeMessage(
      KycBridgeMessage.Message.AcuantProcessFinished(acuantProcessFinished)
    )
    val atalaMessage = AtalaMessage().withKycBridgeMessage(kycBridgeMessage)

    val request = SendMessageRequest(connectionId, atalaMessage.toByteString)

    baseGrpcClientService.authenticatedCall(request, _.sendMessage)
  }

  def sendDocumentPhotoToAcuant(
      documentInstanceId: String,
      bearerToken: String,
      client: Client[Task]
  ): Task[Either[Exception, String]] = {
    val documentPhoto = readFileFromResource("sampleDocument.jpeg")

    val request = POST(
      documentPhoto,
      Uri.unsafeFromString(
        s"https://preview.assureid.acuant.net/AssureIDService/Document/$documentInstanceId/Image?side=0&light=0&metrics=true"
      ),
      Authorization(Credentials.Token(AuthScheme.Bearer, bearerToken))
    )

    runRequestToEither[String](request, client)
  }

  trait E2EFixtures {
    val clientKey: ECKeyPair = ecTrait.generateKeyPair()
    val keyId = "master"
  }
}
