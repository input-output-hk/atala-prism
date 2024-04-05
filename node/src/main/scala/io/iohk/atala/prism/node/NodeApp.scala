package io.iohk.atala.prism.node

import cats.effect.unsafe.IORuntime
import cats.effect.{ExitCode, IO, IOApp, Resource}
import cats.implicits.toFunctorOps
import com.typesafe.config.{Config, ConfigFactory}
import doobie.hikari.HikariTransactor
import io.grpc.{Server, ServerBuilder}
import io.iohk.atala.prism.auth.WhitelistedAuthenticatorF
import io.iohk.atala.prism.auth.grpc.{
  GrpcAuthenticatorInterceptor,
  TraceExposeInterceptor,
  TraceReadInterceptor
}
import io.iohk.atala.prism.auth.utils.DidWhitelistLoader
import io.iohk.atala.prism.identity.PrismDid
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.metrics.UptimeReporter
import io.iohk.atala.prism.models.DidSuffix
import io.iohk.atala.prism.node.cardano.CardanoClient
import io.iohk.atala.prism.node.metrics.NodeReporter
import io.iohk.atala.prism.node.operations.ApplyOperationConfig
import io.iohk.atala.prism.node.repositories._
import io.iohk.atala.prism.node.services.CardanoLedgerService.CardanoBlockHandler
import io.iohk.atala.prism.node.services._
import io.iohk.atala.prism.node.services.models.AtalaObjectNotification
import io.iohk.atala.prism.protos.node_api._
import io.iohk.atala.prism.repositories.{SchemaMigrations, TransactorFactory}
import io.iohk.atala.prism.utils.IOUtils._
import kamon.Kamon
import kamon.module.Module
import org.slf4j.LoggerFactory
import tofu.logging.Logs

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

object NodeApp extends IOApp {

  private val port = 50053

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
      nodeExplorerDids = loadNodeExplorerDids(globalConfig)
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
      keyValueService <- KeyValueService.resource(keyValuesRepository, logs)
      ledger <- createLedger(
        globalConfig,
        keyValueService,
        onCardanoBlock,
        onAtalaObject,
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
      credentialBatchesRepository <-
        CredentialBatchesRepository.resource(liftedTransactor, logs)
      metricsCountersRepository <- MetricsCountersRepository.resource(liftedTransactor, logs)
      nodeService <- NodeService.resource(
        didDataRepository,
        objectManagementService,
        credentialBatchesRepository,
        logs
      )
      nodeStatisticsService <- StatisticsService.resource(atalaOperationsRepository, metricsCountersRepository, logs)
      nodeExplorerService <- NodeExplorerService.resource(ledger, objectManagementService, logs)
      requestNoncesRepo <- RequestNoncesRepository.resource(liftedTransactor, logs)
      authenticator <- WhitelistedAuthenticatorF.resource(new NodeExplorerAuthenticator(requestNoncesRepo), logs)
      nodeExplorerGrpcService = new NodeExplorerGrpcServiceImpl(
        authenticator,
        nodeExplorerService,
        nodeStatisticsService,
        nodeExplorerDids
      )
      nodeGrpcService = new NodeGrpcServiceImpl(nodeService)
      server <- startServer(nodeGrpcService, nodeExplorerGrpcService)
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

  private def loadNodeExplorerDids(config: Config): Set[PrismDid] = {
    logger.info("Loading DID whitelist")
    val didWhitelist = DidWhitelistLoader.load(config, "nodeExplorer")
    if (didWhitelist.isEmpty) {
      logger.warn(
        s"DID whitelist is empty, which makes explorer methods inaccessible"
      )
    } else {
      logger.info(
        s"DID whitelist:\n${didWhitelist.map(_.getValue).map("- " + _).mkString("\n")}"
      )
    }
    didWhitelist
  }

  private def initializeCardano(
      keyValueService: KeyValueService[IOWithTraceIdContext],
      globalConfig: Config,
      onCardanoBlock: CardanoBlockHandler[IOWithTraceIdContext],
      onAtalaObject: AtalaObjectNotification => IOWithTraceIdContext[Unit],
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

  private def createLedger(
      config: Config,
      keyValueService: KeyValueService[IOWithTraceIdContext],
      onCardanoBlock: CardanoBlockHandler[IOWithTraceIdContext],
      onAtalaObject: AtalaObjectNotification => IOWithTraceIdContext[Unit],
      logs: Logs[IO, IOWithTraceIdContext]
  ): Resource[IO, UnderlyingLedger[IOWithTraceIdContext]] =
    config.getString("ledger") match {
      case "cardano" =>
        initializeCardano(
          keyValueService,
          config,
          onCardanoBlock,
          onAtalaObject,
          logs
        )
      case "in-memory" =>
        logger.info("Using in-memory ledger")
        InMemoryLedgerService.resource(onAtalaObject, logs)
    }

  private def startServer(
      nodeService: NodeGrpcServiceImpl,
      nodeExplorerService: NodeExplorerGrpcServiceImpl,
  ): Resource[IO, Server] =
    Resource.make[IO, Server](IO {
      logger.info("Starting server")
      import io.grpc.protobuf.services.ProtoReflectionService
      val server = ServerBuilder
        .forPort(NodeApp.port)
        .intercept(new TraceExposeInterceptor)
        .intercept(new TraceReadInterceptor)
        .intercept(new GrpcAuthenticatorInterceptor)
        .addService(NodeServiceGrpc.bindService(nodeService, executionContext))
        .addService(NodeExplorerServiceGrpc.bindService(nodeExplorerService, executionContext))
        .addService(
          _root_.grpc.health.v1.health.HealthGrpc
            .bindService(new HealthService, executionContext)
        )
        .addService(
          ProtoReflectionService.newInstance()
        ) // TODO: Decide before release if we should keep this (or guard it with a config flag)
        .build()
        .start()
      logger.info("Server started, listening on " + NodeApp.port)
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
