package io.iohk.atala.prism.node

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.Resource
import cats.effect.unsafe.IORuntime
import cats.implicits.toFunctorOps
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import doobie.hikari.HikariTransactor
import io.grpc.Server
import io.grpc.ServerBuilder
import io.iohk.atala.prism.node.cardano.CardanoClient
import io.iohk.atala.prism.node.logging.TraceId
import io.iohk.atala.prism.node.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.node.metrics.NodeReporter
import io.iohk.atala.prism.node.metrics.UptimeReporter
import io.iohk.atala.prism.node.models.DidSuffix
import io.iohk.atala.prism.node.operations.ApplyOperationConfig
import io.iohk.atala.prism.node.repositories.SchemaMigrations
import io.iohk.atala.prism.node.repositories.TransactorFactory
import io.iohk.atala.prism.node.repositories._
import io.iohk.atala.prism.node.services.CardanoLedgerService.CardanoBlockHandler
import io.iohk.atala.prism.node.services._
import io.iohk.atala.prism.node.services.models.{AtalaObjectBulkNotificationHandler, AtalaObjectNotification}
import io.iohk.atala.prism.node.utils.IOUtils._
import io.iohk.atala.prism.protos.node_api._
import kamon.Kamon
import kamon.module.Module
import org.slf4j.LoggerFactory
import tofu.logging.Logs

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

object NodeApp extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    new NodeApp(ExecutionContext.global).start().use(_ => IO.never)
}

class NodeApp(executionContext: ExecutionContext) { self =>

  implicit val implicitExecutionContext: ExecutionContext = executionContext

  implicit val runtime: IORuntime = IORuntime.global

  private val logger = LoggerFactory.getLogger(this.getClass)

  private def start(): Resource[IO, (SubmissionSchedulingService, Server)] = {
    for {
      globalConfig <- loadConfig()
      _ <- startMetrics(globalConfig)
      databaseConfig = TransactorFactory.transactorConfig(globalConfig)
      _ = applyDatabaseMigrations(databaseConfig)
      transactor <- connectToTheDb(databaseConfig)
      liftedTransactor = transactor.mapK(TraceId.liftToIOWithTraceId)
      logs = Logs.withContext[IO, IOWithTraceIdContext]
      protocolVersionRepository <- ProtocolVersionRepository.resource(
        liftedTransactor,
        logs
      )
      keyValuesRepository <- KeyValuesRepository.resource(
        liftedTransactor,
        logs
      )
      trustedProposer = DidSuffix(globalConfig.getString("trustedProposerSuffix"))
      _ <- Resource.pure { logger.info(s"Trusted DID suffix $trustedProposer") }
      blockProcessingService = new BlockProcessingServiceImpl(ApplyOperationConfig(trustedProposer))
      atalaOperationsRepository <- AtalaOperationsRepository.resource(
        liftedTransactor,
        logs
      )
      atalaObjectsTransactionsRepository <- AtalaObjectsTransactionsRepository
        .resource(liftedTransactor, logs)
      didPublicKeysLimit = globalConfig.getInt("didPublicKeysLimit")
      didServicesLimit = globalConfig.getInt("didServicesLimit")
      objectManagementService <- ObjectManagementService.resource(
        atalaOperationsRepository,
        atalaObjectsTransactionsRepository,
        keyValuesRepository,
        protocolVersionRepository,
        blockProcessingService,
        didPublicKeysLimit,
        didServicesLimit,
        liftedTransactor,
        logs
      )
      onCardanoBlock = onCardanoBlockOp(protocolVersionRepository)
      onAtalaObject = onAtalaObjectOp(objectManagementService)
      onAtalaObjectBulk = onAtalaObjectBulkOp(objectManagementService)
      keyValueService <- KeyValueService.resource(keyValuesRepository, logs)
      ledger <- createLedger(
        globalConfig,
        keyValueService,
        onCardanoBlock,
        onAtalaObject,
        onAtalaObjectBulk,
        logs
      )
      didDataRepository <- DIDDataRepository.resource(liftedTransactor, logs)
      refreshAndSubmitPeriod = FiniteDuration(
        globalConfig.getDuration("refreshAndSubmitPeriod").toNanos,
        TimeUnit.NANOSECONDS
      )
      moveScheduledToPendingPeriod = FiniteDuration(
        globalConfig.getDuration("moveScheduledToPendingPeriod").toNanos,
        TimeUnit.NANOSECONDS
      )
      transactionsPerSecond = globalConfig.getInt("transactionsPerSecond")
      submissionService <- SubmissionService.resource(
        ledger,
        atalaOperationsRepository,
        atalaObjectsTransactionsRepository,
        SubmissionService.Config(
          maxNumberTransactionsToSubmit = moveScheduledToPendingPeriod.toSeconds.toInt * transactionsPerSecond
        ),
        logs
      )
      submissionSchedulingService = SubmissionSchedulingService(
        SubmissionSchedulingService.Config(
          refreshAndSubmitPeriod = refreshAndSubmitPeriod,
          moveScheduledToPendingPeriod = moveScheduledToPendingPeriod
        ),
        submissionService
      )
      nodeService <- NodeService.resource(
        didDataRepository,
        objectManagementService,
        logs
      )
      nodeGrpcService = new NodeGrpcServiceImpl(nodeService)
      port = globalConfig.getInt("port")
      server <- startServer(nodeGrpcService, port)
    } yield (submissionSchedulingService, server)
  }

  private def startMetrics(config: Config): Resource[IO, Module.Registration] =
    Resource.make(IO {
      Kamon.init()
      Kamon.addReporter("uptime", new UptimeReporter(config))
    })(_ => IO.fromFuture(IO(Kamon.stop())))

  private def loadConfig(): Resource[IO, Config] = Resource.pure[IO, Config] {
    logger.info("Loading config")
    ConfigFactory.load()
  }

  private def initializeCardano(
      keyValueService: KeyValueService[IOWithTraceIdContext],
      globalConfig: Config,
      onCardanoBlock: CardanoBlockHandler[IOWithTraceIdContext],
      onAtalaObject: AtalaObjectNotification => IOWithTraceIdContext[Unit],
      onAtalaObjectBulk: AtalaObjectBulkNotificationHandler[IOWithTraceIdContext],
      logs: Logs[IO, IOWithTraceIdContext]
  ): Resource[IO, UnderlyingLedger[IOWithTraceIdContext]] = {
    val config = NodeConfig.cardanoConfig(globalConfig.getConfig("cardano"))
    createCardanoClient(config.cardanoClientConfig, logs).flatMap { cardanoClient =>
      Kamon.addReporter(
        "node-reporter",
        NodeReporter(config, cardanoClient, keyValueService)
      )
      CardanoLedgerService.resource[IOWithTraceIdContext, IO](
        config,
        cardanoClient,
        keyValueService,
        onCardanoBlock,
        onAtalaObject,
        onAtalaObjectBulk,
        logs
      )
    }
  }

  private def createCardanoClient(
      cardanoClientConfig: CardanoClient.Config,
      logs: Logs[IO, IOWithTraceIdContext]
  ): Resource[IO, CardanoClient[IOWithTraceIdContext]] = {
    logger.info("Creating cardano client")
    CardanoClient
      .makeResource[IO, IOWithTraceIdContext](cardanoClientConfig, logs)
      .mapK(TraceId.unLiftIOWithTraceId())
  }

  private def connectToTheDb(
      dbConfig: TransactorFactory.Config
  ): Resource[IO, HikariTransactor[IO]] = {
    logger.info("Connecting to the database")
    TransactorFactory.transactor[IO](dbConfig)
  }

  private def onCardanoBlockOp(
      in: ProtocolVersionRepository[IOWithTraceIdContext]
  ): CardanoBlockHandler[IOWithTraceIdContext] = block => in.markEffective(block.header.blockNo).void

  private def onAtalaObjectOp(
      objectManagementService: ObjectManagementService[IOWithTraceIdContext]
  ): AtalaObjectNotification => IOWithTraceIdContext[Unit] = notification => {
    objectManagementService
      .saveObject(notification)
      .void
  }

  private def onAtalaObjectBulkOp(
      objectManagementService: ObjectManagementService[IOWithTraceIdContext]
  ): AtalaObjectBulkNotificationHandler[IOWithTraceIdContext] = notifications => {
    objectManagementService.saveObjects(notifications).void
  }

  private def createLedger(
      config: Config,
      keyValueService: KeyValueService[IOWithTraceIdContext],
      onCardanoBlock: CardanoBlockHandler[IOWithTraceIdContext],
      onAtalaObject: AtalaObjectNotification => IOWithTraceIdContext[Unit],
      onAtalaObjectBulk: AtalaObjectBulkNotificationHandler[IOWithTraceIdContext],
      logs: Logs[IO, IOWithTraceIdContext]
  ): Resource[IO, UnderlyingLedger[IOWithTraceIdContext]] =
    config.getString("ledger") match {
      case "cardano" =>
        initializeCardano(
          keyValueService,
          config,
          onCardanoBlock,
          onAtalaObject,
          onAtalaObjectBulk,
          logs
        )
      case "in-memory" =>
        logger.info("Using in-memory ledger")
        InMemoryLedgerService.resource(onAtalaObject, logs)
    }

  private def startServer(
      nodeService: NodeGrpcServiceImpl,
      port: Int
  ): Resource[IO, Server] =
    Resource.make[IO, Server](IO {
      logger.info("Starting server")
      import io.grpc.protobuf.services.ProtoReflectionService
      val server = ServerBuilder
        .forPort(port)
        .addService(NodeServiceGrpc.bindService(nodeService, executionContext))
        .addService(
          _root_.grpc.health.v1.health.HealthGrpc
            .bindService(new HealthService, executionContext)
        )
        .addService(
          ProtoReflectionService.newInstance()
        )
        .build()
        .start()
      logger.info("Server started, listening on " + port)
      server
    })(server =>
      IO {
        System.err.println(
          "*** shutting down gRPC server since JVM is shutting down"
        )
        server.shutdown()
        server.awaitTermination()
        System.err.println("*** server shut down")
      }
    )

  private def applyDatabaseMigrations(
      databaseConfig: TransactorFactory.Config
  ): Unit = {
    logger.info("Applying database migrations")
    val appliedMigrations = SchemaMigrations.migrate(databaseConfig)
    if (appliedMigrations == 0) {
      logger.info("Database up to date")
    } else {
      logger.info(s"$appliedMigrations migration scripts applied")
    }
  }
}
