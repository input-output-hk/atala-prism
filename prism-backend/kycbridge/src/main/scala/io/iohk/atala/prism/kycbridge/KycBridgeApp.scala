package io.iohk.atala.prism.kycbridge

import monix.eval.{Task, TaskApp}
import cats.effect.{ExitCode, Resource}
import com.typesafe.config.ConfigFactory
import io.grpc.Server
import io.iohk.atala.prism.config.{ConnectorConfig, NodeConfig}
import io.iohk.atala.prism.connector.RequestAuthenticator
import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.prism.kycbridge.config.KycBridgeConfig
import io.iohk.atala.prism.kycbridge.models.assureId.{Device, DeviceType}
import io.iohk.atala.prism.kycbridge.services.{
  AcasServiceImpl,
  AcuantService,
  AssureIdServiceImpl,
  ConnectionService,
  FaceIdServiceImpl,
  KycBridgeGrpcService,
  KycBridgeService
}
import io.iohk.atala.prism.repositories.TransactorFactory
import io.iohk.atala.prism.services.{ConnectorClientServiceImpl, NodeClientServiceImpl}
import org.http4s.client.blaze.BlazeClientBuilder
import org.slf4j.LoggerFactory
import io.iohk.atala.kycbridge.protos.kycbridge_api.KycBridgeServiceGrpc
import io.iohk.atala.prism.utils.GrpcUtils

import scala.concurrent.ExecutionContext.Implicits.global
import io.iohk.atala.prism.services.ConnectorMessagesService
import doobie.implicits._
import io.iohk.atala.prism.daos.ConnectorMessageOffsetDao
import io.iohk.atala.prism.kycbridge.processors.DocumentUploadedMessageProcessor
import io.iohk.atala.prism.protos.connector_api.ConnectorServiceGrpc
import io.iohk.atala.prism.protos.node_api.NodeServiceGrpc

object KycBridgeApp extends TaskApp {

  implicit val ec = EC

  private val logger = LoggerFactory.getLogger(this.getClass)

  override def run(args: List[String]): Task[ExitCode] = {
    val classLoader = Thread.currentThread().getContextClassLoader
    app(classLoader).use {
      case (assureIdService, acasService, grpcServer) =>
        logger.info("Kyc bridge application started")

        //only for demonstration purpose
        val device = Device(
          `type` = DeviceType(
            manufacturer = "manufacturer",
            model = "model"
          )
        )

        (for {
          documentResponse <- assureIdService.createNewDocumentInstance(device)
          _ = logger.info(s"New document response: $documentResponse")
          accessTokenResponse <- acasService.getAccessToken
          _ = logger.info(s"Access token response: $accessTokenResponse")
          documentStatus <- assureIdService.getDocumentStatus(
            documentResponse.toOption.get.documentId
          )
          _ = logger.info(s"Document status: $documentStatus")
          _ = logger.info("Starting GRPC server")
          _ = grpcServer.start
        } yield ()).flatMap(_ => Task.never)
    }
  }

  def app(classLoader: ClassLoader): Resource[Task, (AssureIdServiceImpl, AcasServiceImpl, Server)] =
    for {
      httpClient <- BlazeClientBuilder[Task](global).resource

      globalConfig = ConfigFactory.load(classLoader)

      kycBridgeConfig = KycBridgeConfig(globalConfig)
      connectorConfig = ConnectorConfig(globalConfig)
      nodeConfig = NodeConfig(globalConfig)
      transactorConfig = TransactorFactory.transactorConfig(globalConfig)

      tx <- TransactorFactory.transactor[Task](transactorConfig)
      _ <- TransactorFactory.runDbMigrations[Task](tx, classLoader)

      // connector
      connector = GrpcUtils.createPlaintextStub(
        host = connectorConfig.host,
        port = connectorConfig.port,
        stub = ConnectorServiceGrpc.stub
      )

      // node
      node = GrpcUtils.createPlaintextStub(
        host = nodeConfig.host,
        port = nodeConfig.port,
        stub = NodeServiceGrpc.stub
      )

      connectorService = new ConnectorClientServiceImpl(connector, new RequestAuthenticator(ec), connectorConfig)
      nodeService = new NodeClientServiceImpl(node, connectorConfig.authConfig)
      kycBridgeService = new KycBridgeService(tx, connectorService)
      kycBridgeGrpcService = new KycBridgeGrpcService(kycBridgeService)(scheduler)
      assureIdService = new AssureIdServiceImpl(kycBridgeConfig.acuantConfig, httpClient)
      acasService = new AcasServiceImpl(kycBridgeConfig.acuantConfig, httpClient)
      faceIdService = new FaceIdServiceImpl(kycBridgeConfig.acuantConfig, httpClient)
      connectionService = new ConnectionService(tx, connectorService)
      acuantService = new AcuantService(tx, assureIdService, acasService, connectorService)

      // connector message processors
      documentUploadedMessageProcessor = new DocumentUploadedMessageProcessor(
        tx,
        nodeService,
        connectorService,
        assureIdService,
        faceIdService,
        connectorConfig
      )

      connectorMessageService = new ConnectorMessagesService(
        connectorService = connectorService,
        messageProcessors = List(documentUploadedMessageProcessor.processor),
        findLastMessageOffset = ConnectorMessageOffsetDao.findLastMessageOffset().transact(tx),
        saveMessageOffset = messageId => ConnectorMessageOffsetDao.updateLastMessageOffset(messageId).transact(tx).void
      )

      _ <- Resource.liftF(connectionService.connectionUpdateStream.compile.drain.start)
      _ <- Resource.liftF(acuantService.acuantDataStream.compile.drain.start)
      _ <- Resource.liftF(connectorMessageService.messagesUpdatesStream.compile.drain.start)

      // gRPC server
      grpcServer <- GrpcUtils.createGrpcServer[Task](
        kycBridgeConfig.grpcConfig,
        sslConfigOption = None,
        KycBridgeServiceGrpc.bindService(kycBridgeGrpcService, scheduler)
      )

    } yield (assureIdService, acasService, grpcServer)

}
