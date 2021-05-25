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
import io.iohk.atala.prism.task.lease.system.{
  ProcessingTaskRouterImpl,
  ProcessingTaskScheduler,
  ProcessingTaskServiceImpl
}
import io.iohk.atala.prism.protos.connector_api.ConnectorServiceGrpc
import io.iohk.atala.prism.protos.node_api.NodeServiceGrpc
import io.iohk.atala.prism.services.BaseGrpcClientService

import java.util.UUID

object KycBridgeApp extends TaskApp {

  implicit val ec = EC

  private val logger = LoggerFactory.getLogger(this.getClass)

  override def run(args: List[String]): Task[ExitCode] = {
    val classLoader = Thread.currentThread().getContextClassLoader
    app(classLoader).use {
      case (assureIdService, acasService, grpcServer, streams) =>
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
          //We use Task.parSequence instead of calling `start` on every task,
          //because `start` doesn't propagate errors, `Task.parSequence` does this.
          _ <- Task.parSequence(streams)
        } yield ())
          .flatMap(_ => Task.never)
          .onErrorHandle(error => {
            logger.error("KYC Bridge exiting because of runaway exception", error)
            ExitCode.Error
          })
    }
  }

  def app(classLoader: ClassLoader): Resource[Task, (AssureIdServiceImpl, AcasServiceImpl, Server, List[Task[Unit]])] =
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

      // auth config - stored in the db
      authConfig <- BaseGrpcClientService.DidBasedAuthConfig.getOrCreate(globalConfig, tx, connector)

      connectorService = new ConnectorClientServiceImpl(connector, new RequestAuthenticator(ec), authConfig)
      nodeService = new NodeClientServiceImpl(node, authConfig)
      kycBridgeService = new KycBridgeService(tx, connectorService)
      kycBridgeGrpcService = new KycBridgeGrpcService(kycBridgeService)(scheduler)
      assureIdService = new AssureIdServiceImpl(kycBridgeConfig.acuantConfig, httpClient)
      acasService = new AcasServiceImpl(kycBridgeConfig.acuantConfig, httpClient)
      faceIdService = new FaceIdServiceImpl(kycBridgeConfig.acuantConfig, httpClient)
      connectionService = new ConnectionService(tx, connectorService)
      acuantService = new AcuantService(tx, assureIdService, acasService, connectorService)
      processingTaskService = new ProcessingTaskServiceImpl(tx, UUID.randomUUID())
      processingTaskRouter = new ProcessingTaskRouterImpl(processingTaskService)
      processingTaskScheduler =
        new ProcessingTaskScheduler(processingTaskService, processingTaskRouter, kycBridgeConfig.taskLeaseConfig)

      // connector message processors
      documentUploadedMessageProcessor = new DocumentUploadedMessageProcessor(
        tx,
        nodeService,
        assureIdService,
        faceIdService,
        authConfig
      )

      connectorMessageService = new ConnectorMessagesService(
        connectorService = connectorService,
        messageProcessors = List(documentUploadedMessageProcessor.processor),
        findLastMessageOffset = ConnectorMessageOffsetDao.findLastMessageOffset().transact(tx),
        saveMessageOffset = messageId => ConnectorMessageOffsetDao.updateLastMessageOffset(messageId).transact(tx).void
      )

      streams = List(
        connectionService.connectionUpdateStream.compile.drain,
        acuantService.acuantDataStream.compile.drain,
        connectorMessageService.messagesUpdatesStream.compile.drain,
        processingTaskScheduler.run.void
      )

      // gRPC server
      grpcServer <- GrpcUtils.createGrpcServer[Task](
        kycBridgeConfig.grpcConfig,
        sslConfigOption = None,
        interceptor = None,
        KycBridgeServiceGrpc.bindService(kycBridgeGrpcService, scheduler)
      )

    } yield (assureIdService, acasService, grpcServer, streams)

}
