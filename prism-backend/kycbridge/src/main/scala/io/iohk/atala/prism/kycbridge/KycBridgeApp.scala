package io.iohk.atala.prism.kycbridge

import cats.effect.{ExitCode, Resource}
import com.typesafe.config.ConfigFactory
import doobie.implicits._
import io.grpc.Server
import io.iohk.atala.kycbridge.protos.kycbridge_api.KycBridgeServiceGrpc
import io.iohk.atala.prism.config.{ConnectorConfig, NodeConfig}
import io.iohk.atala.prism.connector.RequestAuthenticator
import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.prism.daos.ConnectorMessageOffsetDao
import io.iohk.atala.prism.kycbridge.config.KycBridgeConfig
import io.iohk.atala.prism.kycbridge.message.processors.{
  AcuantDocumentUploadedMessageProcessor,
  SendForAcuantManualReviewMessageProcessor,
  RequestAcuantProcessMessageProcessor
}
import io.iohk.atala.prism.kycbridge.services._
import io.iohk.atala.prism.kycbridge.task.lease.system.KycBridgeProcessingTaskState
import io.iohk.atala.prism.kycbridge.task.lease.system.processors._
import io.iohk.atala.prism.protos.connector_api.ConnectorServiceGrpc
import io.iohk.atala.prism.protos.node_api.NodeServiceGrpc
import io.iohk.atala.prism.repositories.TransactorFactory
import io.iohk.atala.prism.services.{
  BaseGrpcClientService,
  ConnectorClientServiceImpl,
  ConnectorMessagesService,
  NodeClientServiceImpl
}
import io.iohk.atala.prism.task.lease.system.processors.ProcessMessagesStateProcessor
import io.iohk.atala.prism.task.lease.system._
import io.iohk.atala.prism.utils.GrpcUtils
import monix.eval.{Task, TaskApp}
import org.http4s.client.blaze.BlazeClientBuilder
import org.slf4j.LoggerFactory

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

object KycBridgeApp extends TaskApp {

  implicit val ec = EC

  private val logger = LoggerFactory.getLogger(this.getClass)

  override def run(args: List[String]): Task[ExitCode] = {
    val classLoader = Thread.currentThread().getContextClassLoader
    app(classLoader).use {
      case (grpcServer, monixTasks) =>
        logger.info("Kyc bridge application started")
        logger.info("Starting GRPC server")
        grpcServer.start
        //We use Task.parSequence instead of calling `start` on every task,
        //because `start` doesn't propagate errors, `Task.parSequence` does this.
        Task
          .parSequence(monixTasks)
          .flatMap(_ => Task.never)
          .onErrorHandle(error => {
            logger.error("KYC Bridge exiting because of runaway exception", error)
            ExitCode.Error
          })
    }
  }

  def app(classLoader: ClassLoader): Resource[Task, (Server, List[Task[Unit]])] =
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
      identityMindService = new IdentityMindServiceImpl(kycBridgeConfig.acuantConfig.identityMind, httpClient)

      processingTaskDao =
        new ProcessingTaskDao[KycBridgeProcessingTaskState](KycBridgeProcessingTaskState.withNameOption)
      processingTaskService =
        new ProcessingTaskServiceImpl[KycBridgeProcessingTaskState](tx, UUID.randomUUID(), processingTaskDao)

      connectionService = new ConnectionService(tx, connectorService)

      // connector message processors
      documentUploadedMessageProcessor = new AcuantDocumentUploadedMessageProcessor(processingTaskService)
      sendForAcuantManualReviewMessageProcessor = new SendForAcuantManualReviewMessageProcessor(processingTaskService)
      requestAcuantProcessMessageProcessor = new RequestAcuantProcessMessageProcessor(processingTaskService)

      connectorMessageService = new ConnectorMessagesService(
        connectorService = connectorService,
        messageProcessors = List(
          documentUploadedMessageProcessor.processor,
          sendForAcuantManualReviewMessageProcessor.processor,
          requestAcuantProcessMessageProcessor.processor
        ),
        findLastMessageOffset = ConnectorMessageOffsetDao.findLastMessageOffset().transact(tx),
        saveMessageOffset = messageId => ConnectorMessageOffsetDao.updateLastMessageOffset(messageId).transact(tx).void
      )

      // processing task processors
      processMessagesStateProcessor =
        new ProcessMessagesStateProcessor[KycBridgeProcessingTaskState](connectorMessageService)
      acuantFetchDocumentState1Processor = new AcuantFetchDocumentState1Processor(tx, connectorService, assureIdService)
      acuantCompareImagesState2Processor =
        new AcuantCompareImagesState2Processor(connectorService, assureIdService, faceIdService)
      acuantCreateCredentialState3Processor =
        new AcuantCreateCredentialState3Processor(connectorService, nodeService, authConfig)
      acuantStartProcessForConnectionStateProcessor =
        new AcuantStartProcessForConnectionStateProcessor(tx, assureIdService, acasService, connectorService)
      processNewConnectionsStateProcessor = new ProcessNewConnectionsStateProcessor(connectionService)
      sendForAcuantManualReviewStateProcessor = new SendForAcuantManualReviewStateProcessor(
        assureIdService,
        identityMindService,
        kycBridgeConfig.acuantConfig.identityMind
      )
      sendForAcuantManualReviewPendingStateProcessor = new SendForAcuantManualReviewPendingStateProcessor(
        connectorService,
        identityMindService
      )
      sendForAcuantManualReviewReadyStateProcessor = new SendForAcuantManualReviewReadyStateProcessor(
        connectorService,
        nodeService,
        identityMindService,
        authConfig
      )

      processingTaskRouter = new ProcessingTaskRouter[KycBridgeProcessingTaskState] {
        override def process(
            processingTask: ProcessingTask[KycBridgeProcessingTaskState],
            workerNumber: Int
        ): Task[ProcessingTaskResult[KycBridgeProcessingTaskState]] = {
          val processor = processingTask.state match {
            case KycBridgeProcessingTaskState.ProcessConnectorMessagesState => processMessagesStateProcessor
            case KycBridgeProcessingTaskState.AcuantFetchDocumentDataState1 => acuantFetchDocumentState1Processor
            case KycBridgeProcessingTaskState.AcuantCompareImagesState2 => acuantCompareImagesState2Processor
            case KycBridgeProcessingTaskState.AcuantIssueCredentialState3 => acuantCreateCredentialState3Processor
            case KycBridgeProcessingTaskState.AcuantStartProcessForConnection =>
              acuantStartProcessForConnectionStateProcessor
            case KycBridgeProcessingTaskState.ProcessNewConnections => processNewConnectionsStateProcessor
            case KycBridgeProcessingTaskState.SendForAcuantManualReviewState => sendForAcuantManualReviewStateProcessor
            case KycBridgeProcessingTaskState.SendForAcuantManualReviewPendingState =>
              sendForAcuantManualReviewPendingStateProcessor
            case KycBridgeProcessingTaskState.SendForAcuantManualReviewReadyState =>
              sendForAcuantManualReviewReadyStateProcessor
          }
          processor.process(processingTask, workerNumber)
        }
      }

      processingTaskScheduler = new ProcessingTaskScheduler[KycBridgeProcessingTaskState](
        processingTaskService,
        processingTaskRouter,
        kycBridgeConfig.taskLeaseConfig
      )

      monixTasks = List(
        processingTaskScheduler.run.void
      )

      // gRPC server
      grpcServer <- GrpcUtils.createGrpcServer[Task](
        kycBridgeConfig.grpcConfig,
        sslConfigOption = None,
        interceptor = None,
        KycBridgeServiceGrpc.bindService(kycBridgeGrpcService, scheduler)
      )

    } yield (grpcServer, monixTasks)

}
