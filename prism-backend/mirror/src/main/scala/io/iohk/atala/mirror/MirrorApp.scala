package io.iohk.atala.mirror

import cats.effect.{ExitCode, Resource}
import com.typesafe.config.ConfigFactory
import doobie.implicits._
import io.grpc.Server
import io.iohk.atala.mirror.config.{CardanoConfig, MirrorConfig}
import io.iohk.atala.mirror.http.ApiServer
import io.iohk.atala.mirror.http.endpoints.PaymentEndpoints
import io.iohk.atala.mirror.models.{CardanoAddress, LovelaceAmount, TrisaVaspAddress}
import io.iohk.atala.mirror.protos.mirror_api.MirrorServiceGrpc
import io.iohk.atala.mirror.protos.trisa.TrisaPeer2PeerGrpc
import io.iohk.atala.mirror.services._
import io.iohk.atala.mirror.task.lease.system.MirrorProcessingTaskState
import io.iohk.atala.mirror.task.lease.system.processors.{
  ProcessNewConnectionsStateProcessor,
  WatchCardanoBlockchainAddressesProcessor
}
import io.iohk.atala.prism.config.{ConnectorConfig, NodeConfig}
import io.iohk.atala.prism.connector.RequestAuthenticator
import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.prism.daos.ConnectorMessageOffsetDao
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
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import monix.eval.{Task, TaskApp}
import monix.execution.Scheduler.Implicits.global
import org.slf4j.LoggerFactory

import java.util.UUID

object MirrorApp extends TaskApp {

  private val logger = LoggerFactory.getLogger(this.getClass)

  case class AppComponents(
      grpcServer: Server,
      trisaGrpcServer: Option[Server],
      monixTasks: List[Task[Unit]]
  )

  /**
    * Run the Mirror application.
    */
  override def run(args: List[String]): Task[ExitCode] = {
    //for some unknown reasons, incorrect class loader is used during execution
    //of Resource "for comprehension".
    val classLoader = Thread.currentThread().getContextClassLoader
    app(classLoader).use {
      case AppComponents(grpcServer, trisaGrpcServer, monixTasks) =>
        logger.info("Starting GRPC server")
        grpcServer.start
        trisaGrpcServer.foreach(_.start())
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

  /**
    * This is an entry point for the Mirror application.
    * The resource contains a GRPC [[Server]] instance, that isn't started.
    */
  def app(classLoader: ClassLoader): Resource[Task, AppComponents] =
    for {
      globalConfig <- Resource.eval(Task {
        logger.info("Loading config")
        ConfigFactory.load(classLoader)
      })

      // configs
      mirrorConfig = MirrorConfig(globalConfig)
      transactorConfig = TransactorFactory.transactorConfig(globalConfig)
      cardanoDBSyncTransactorConfig = TransactorFactory.transactorConfig(globalConfig.getConfig("cardano"))
      connectorConfig = ConnectorConfig(globalConfig)
      nodeConfig = NodeConfig(globalConfig)
      cardanoConfig = CardanoConfig(globalConfig)

      // db
      tx <- TransactorFactory.transactor[Task](transactorConfig)
      _ <- TransactorFactory.runDbMigrations[Task](tx, classLoader)
      cardanoDBSyncTransactor <- TransactorFactory.transactor[Task](cardanoDBSyncTransactorConfig)

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

      // services
      connectorService = new ConnectorClientServiceImpl(connector, new RequestAuthenticator(EC), authConfig)
      nodeService = new NodeClientServiceImpl(node, authConfig)
      mirrorServiceImpl = new MirrorServiceImpl(tx, connectorService)
      credentialService = new CredentialService(tx, connectorService, nodeService)
      cardanoAddressInfoService = new CardanoAddressInfoService(tx, mirrorConfig.httpConfig, nodeService)
      mirrorGrpcService = new MirrorGrpcService(mirrorServiceImpl)(scheduler)
      cardanoAddressService = new CardanoAddressService()
      trisaIntegrationService =
        if (mirrorConfig.trisaConfig.enabled) {
          new TrisaIntegrationServiceImpl(mirrorConfig.trisaConfig)
        } else {
          new TrisaIntegrationServiceDisabledImpl
        }
      trisaService = new TrisaService(trisaIntegrationService)
      cardanoDeterministicWalletsService =
        new CardanoDeterministicWalletsService(tx, cardanoDBSyncTransactor, cardanoAddressService, cardanoConfig)

      connectorMessageService = new ConnectorMessagesService(
        connectorService = connectorService,
        messageProcessors = List(
          credentialService.credentialMessageProcessor,
          cardanoAddressInfoService.cardanoAddressInfoMessageProcessor,
          cardanoAddressInfoService.payIdMessageProcessor,
          cardanoAddressInfoService.payIdNameRegistrationMessageProcessor,
          cardanoAddressInfoService.checkPayIdNameAvailabilityMessageProcessor,
          cardanoAddressInfoService.getPayIdNameMessageProcessor,
          cardanoAddressInfoService.getPayIdAddressesMessageProcessor,
          cardanoAddressInfoService.getRegisteredWalletsMessageProcessor,
          trisaService.initiateTrisaCardanoTransactionMessageProcessor,
          cardanoDeterministicWalletsService.registerWalletMessageProcessor
        ),
        findLastMessageOffset = ConnectorMessageOffsetDao
          .findLastMessageOffset()
          .logSQLErrors("finding last message offset", logger)
          .transact(tx),
        saveMessageOffset = messageId =>
          ConnectorMessageOffsetDao
            .updateLastMessageOffset(messageId)
            .logSQLErrors("updating last message offset", logger)
            .transact(tx)
            .void
      )
      trisaPeer2PeerService = new TrisaPeer2PeerService(mirrorServiceImpl)

      //trisa
      _ = if (mirrorConfig.trisaConfig.enabled)
        trisaIntegrationService
          .initiateTransaction(
            CardanoAddress("source"),
            CardanoAddress("destination"),
            LovelaceAmount(10),
            TrisaVaspAddress("vasp1", 8091)
          )
          .runSyncUnsafe() match {
          case Right(_) => logger.info("Received successful response")
          case Left(error) =>
            logger.warn(s"Error occurred when sending transaction to vasp ${error.toStatus.getDescription}")
        }

      // gRPC server
      grpcServer <- GrpcUtils.createGrpcServer[Task](
        mirrorConfig.grpcConfig,
        sslConfigOption = None,
        interceptor = None,
        MirrorServiceGrpc.bindService(mirrorGrpcService, scheduler)
      )

      // gRPC server
      trisaGrpcServer <-
        if (mirrorConfig.trisaConfig.enabled)
          GrpcUtils
            .createGrpcServer[Task](
              mirrorConfig.trisaConfig.grpcConfig,
              sslConfigOption = Some(mirrorConfig.trisaConfig.sslConfig),
              interceptor = None,
              TrisaPeer2PeerGrpc.bindService(trisaPeer2PeerService, scheduler)
            )
            .map(Some(_))
        else Resource.pure[Task, Option[Server]](None)

      paymentsEndpoint = new PaymentEndpoints(cardanoAddressInfoService, mirrorConfig.httpConfig)
      apiServer = new ApiServer(paymentsEndpoint, mirrorConfig.httpConfig)
      _ <- apiServer.payIdServer.resource

      processingTaskDao = new ProcessingTaskDao[MirrorProcessingTaskState](MirrorProcessingTaskState.withNameOption)
      processingTaskService =
        new ProcessingTaskServiceImpl[MirrorProcessingTaskState](tx, UUID.randomUUID(), processingTaskDao)

      watchCardanoBlockchainAddressesProcessor = new WatchCardanoBlockchainAddressesProcessor(
        tx,
        cardanoDBSyncTransactor,
        cardanoConfig,
        cardanoAddressService,
        processingTaskService
      )
      processMessagesStateProcessor =
        new ProcessMessagesStateProcessor[MirrorProcessingTaskState](connectorMessageService)
      processNewConnectionsStateProcessor = new ProcessNewConnectionsStateProcessor(credentialService)

      processingTaskRouter = new ProcessingTaskRouter[MirrorProcessingTaskState] {
        override def process(
            processingTask: ProcessingTask[MirrorProcessingTaskState],
            workerNumber: Int
        ): Task[ProcessingTaskResult[MirrorProcessingTaskState]] = {

          val processor = processingTask.state match {
            case MirrorProcessingTaskState.WatchCardanoBlockchainAddressesState =>
              watchCardanoBlockchainAddressesProcessor
            case MirrorProcessingTaskState.ProcessConnectorMessagesState => processMessagesStateProcessor
            case MirrorProcessingTaskState.ProcessNewConnections => processNewConnectionsStateProcessor
          }

          processor.process(processingTask, workerNumber)
        }
      }

      processingTaskScheduler = new ProcessingTaskScheduler[MirrorProcessingTaskState](
        processingTaskService,
        processingTaskRouter,
        mirrorConfig.taskLeaseConfig
      )

      monixTasks = List(
        processingTaskScheduler.run.void
      )

    } yield AppComponents(grpcServer, trisaGrpcServer, monixTasks)

}
