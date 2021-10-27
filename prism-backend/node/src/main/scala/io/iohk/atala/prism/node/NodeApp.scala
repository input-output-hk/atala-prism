package io.iohk.atala.prism.node

import cats.effect.{ContextShift, IO}
import cats.implicits.toFunctorOps
import com.typesafe.config.{Config, ConfigFactory}
import io.grpc.{Server, ServerBuilder}
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.metrics.UptimeReporter
import io.iohk.atala.prism.node.cardano.CardanoClient
import io.iohk.atala.prism.node.metrics.NodeReporter
import io.iohk.atala.prism.node.repositories._
import io.iohk.atala.prism.node.services.CardanoLedgerService.CardanoBlockHandler
import io.iohk.atala.prism.node.services._
import io.iohk.atala.prism.node.services.models.AtalaObjectNotification
import io.iohk.atala.prism.protos.node_api._
import io.iohk.atala.prism.repositories.{SchemaMigrations, TransactorFactory}
import io.iohk.atala.prism.utils.IOUtils._
import kamon.Kamon
import monix.execution.Scheduler.Implicits.{global => scheduler}
import org.slf4j.LoggerFactory
import tofu.logging.Logs

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{Await, ExecutionContext, Future, Promise}

object NodeApp {

  def main(args: Array[String]): Unit = {
    val server = new NodeApp(ExecutionContext.global)
    server.start()
    server.blockUntilShutdown()
  }

  private val port = 50053

}

class NodeApp(executionContext: ExecutionContext) { self =>

  implicit val implicitExecutionContext: ExecutionContext = executionContext

  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  private val logger = LoggerFactory.getLogger(this.getClass)

  private val logs = Logs.withContext[IO, IOWithTraceIdContext]

  private[this] var server: Server = _

  private def start(): Unit = {
    Kamon.init()
    logger.info("Loading config")
    val globalConfig = ConfigFactory.load()
    val databaseConfig = TransactorFactory.transactorConfig(globalConfig)

    logger.info("Setting-up uptime metrics")
    Kamon.registerModule("uptime", new UptimeReporter(globalConfig))

    logger.info("Applying database migrations")
    applyDatabaseMigrations(databaseConfig)

    logger.info("Connecting to the database")
    implicit val (transactor, releaseTransactor) =
      TransactorFactory.transactor[IO](databaseConfig).allocated.unsafeRunSync()

    val liftedTransactor = transactor.mapK(TraceId.liftToIOWithTraceId)

    val objectManagementServicePromise: Promise[ObjectManagementService] =
      Promise()

    val protocolVersionRepository =
      ProtocolVersionRepository.unsafe(liftedTransactor, logs)
    val onCardanoBlock: CardanoBlockHandler = block =>
      {
        protocolVersionRepository.markEffective(block.header.blockNo).void
      }.run(TraceId.generateYOLO)
        .unsafeToFuture()

    def onAtalaObject(notification: AtalaObjectNotification): Future[Unit] = {
      objectManagementServicePromise.future.map { objectManagementService =>
        objectManagementService.saveObject(notification)
        ()
      }
    }

    val keyValuesRepository = KeyValuesRepository.unsafe(liftedTransactor, logs)
    val keyValueService = KeyValueService.unsafe(keyValuesRepository, logs)

    val (atalaReferenceLedger, releaseAtalaReferenceLedger) =
      globalConfig.getString("ledger") match {
        case "cardano" =>
          initializeCardano(
            keyValueService,
            globalConfig,
            onCardanoBlock,
            onAtalaObject,
            logs
          )
        case "in-memory" =>
          logger.info("Using in-memory ledger")
          (new InMemoryLedgerService(onAtalaObject), None)
      }
    logger.info("Creating blocks processor")
    val blockProcessingService = new BlockProcessingServiceImpl
    val didDataRepository = DIDDataRepository.unsafe(liftedTransactor, logs)
    val atalaOperationsRepository =
      AtalaOperationsRepository.unsafe(liftedTransactor, logs)
    val atalaObjectsTransactionsRepository =
      AtalaObjectsTransactionsRepository.unsafe(liftedTransactor, logs)

    val ledgerPendingTransactionTimeout =
      globalConfig.getDuration("ledgerPendingTransactionTimeout")
    val transactionRetryPeriod = FiniteDuration(
      globalConfig.getDuration("transactionRetryPeriod").toNanos,
      TimeUnit.NANOSECONDS
    )
    val operationSubmissionPeriod = FiniteDuration(
      globalConfig.getDuration("operationSubmissionPeriod").toNanos,
      TimeUnit.NANOSECONDS
    )
    val transactionsPerSecond = globalConfig.getInt("transactionsPerSecond")
    val submissionService = SubmissionService.unsafe(
      atalaReferenceLedger,
      atalaOperationsRepository,
      atalaObjectsTransactionsRepository,
      SubmissionService.Config(
        maxNumberTransactionsToSubmit = operationSubmissionPeriod.toSeconds.toInt * transactionsPerSecond,
        maxNumberTransactionsToRetry = transactionRetryPeriod.toSeconds.toInt * transactionsPerSecond
      ),
      logs
    )
    val submissionSchedulingService = SubmissionSchedulingService(
      SubmissionSchedulingService.Config(
        ledgerPendingTransactionTimeout = ledgerPendingTransactionTimeout,
        transactionRetryPeriod = transactionRetryPeriod,
        operationSubmissionPeriod = operationSubmissionPeriod
      ),
      submissionService
    )

    val objectManagementService = ObjectManagementService(
      atalaOperationsRepository,
      atalaObjectsTransactionsRepository,
      keyValuesRepository,
      protocolVersionRepository,
      blockProcessingService
    )
    objectManagementServicePromise.success(objectManagementService)

    val credentialBatchesRepository =
      CredentialBatchesRepository.unsafe(liftedTransactor, logs)

    val nodeService =
      new NodeServiceImpl(
        didDataRepository,
        objectManagementService,
        submissionSchedulingService,
        credentialBatchesRepository
      )

    logger.info("Starting server")
    import io.grpc.protobuf.services.ProtoReflectionService
    server = ServerBuilder
      .forPort(NodeApp.port)
      .addService(NodeServiceGrpc.bindService(nodeService, executionContext))
      .addService(
        _root_.grpc.health.v1.health.HealthGrpc
          .bindService(new HealthService, executionContext)
      )
      .addService(
        ProtoReflectionService.newInstance()
      ) //TODO: Decide before release if we should keep this (or guard it with a config flag)
      .build()
      .start()

    logger.info("Server started, listening on " + NodeApp.port)
    sys.addShutdownHook {
      System.err.println(
        "*** shutting down gRPC server since JVM is shutting down"
      )
      releaseTransactor.unsafeRunSync()
      releaseAtalaReferenceLedger.foreach(_.unsafeRunSync())
      self.stop()
      Await.result(Kamon.stop(), Duration.Inf)
      System.err.println("*** server shut down")
    }
    ()
  }

  private def initializeCardano(
      keyValueService: KeyValueService[IOWithTraceIdContext],
      globalConfig: Config,
      onCardanoBlock: CardanoBlockHandler,
      onAtalaObject: AtalaObjectNotification => Future[Unit],
      logs: Logs[IO, IOWithTraceIdContext]
  ): (CardanoLedgerService, Option[IO[Unit]]) = {
    val config = NodeConfig.cardanoConfig(globalConfig.getConfig("cardano"))
    val (cardanoClient, releaseClient) =
      createCardanoClient(config.cardanoClientConfig, logs)
    Kamon.registerModule(
      "node-reporter",
      NodeReporter(config, cardanoClient, keyValueService)
    )
    val cardano = CardanoLedgerService(
      config,
      cardanoClient,
      keyValueService,
      onCardanoBlock,
      onAtalaObject
    )
    (cardano, Some(releaseClient))
  }

  private def createCardanoClient(
      cardanoClientConfig: CardanoClient.Config,
      logs: Logs[IO, IOWithTraceIdContext]
  ): (CardanoClient[IOWithTraceIdContext], IO[Unit]) = {
    logger.info("Creating cardano client")
    CardanoClient
      .makeResource[IO, IOWithTraceIdContext](cardanoClientConfig, logs)
      .mapK(TraceId.unLiftIOWithTraceId())
      .allocated
      .unsafeRunSync()
  }

  private def stop(): Unit = {
    if (server != null) {
      server.shutdown()
    }
    ()
  }

  private def blockUntilShutdown(): Unit = {
    if (server != null) {
      server.awaitTermination()
    }
  }

  private def applyDatabaseMigrations(
      databaseConfig: TransactorFactory.Config
  ): Unit = {
    val appliedMigrations = SchemaMigrations.migrate(databaseConfig)
    if (appliedMigrations == 0) {
      logger.info("Database up to date")
    } else {
      logger.info(s"$appliedMigrations migration scripts applied")
    }
  }
}
