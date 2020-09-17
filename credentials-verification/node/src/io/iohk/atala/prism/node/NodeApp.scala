package io.iohk.atala.prism.node

import com.typesafe.config.{Config, ConfigFactory}
import io.grpc.{Server, ServerBuilder}
import io.iohk.atala.prism.node.bitcoin.BitcoinClient
import io.iohk.atala.prism.node.objects.{ObjectStorageService, S3ObjectStorageService}
import io.iohk.atala.prism.node.repositories.{CredentialsRepository, DIDDataRepository, KeyValuesRepository}
import io.iohk.atala.prism.node.services.AtalaService.BitcoinNetwork
import io.iohk.atala.prism.node.services._
import io.iohk.atala.prism.node.services.models.{AtalaObjectNotification, AtalaObjectNotificationHandler}
import io.iohk.atala.prism.repositories.{SchemaMigrations, TransactorFactory}
import io.iohk.prism.protos.node_api._
import monix.execution.Scheduler.Implicits.{global => scheduler}
import org.slf4j.LoggerFactory
import software.amazon.awssdk.regions.Region

import scala.concurrent.{ExecutionContext, Future, Promise}

/**
  * Run with `mill -i node.run`, otherwise, the server will stay running even after ctrl+C.
  */
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

  private val logger = LoggerFactory.getLogger(this.getClass)

  private[this] var server: Server = null

  private def start(): Unit = {
    logger.info("Loading config")
    val globalConfig = ConfigFactory.load()
    val databaseConfig = NodeConfig.transactorConfig(globalConfig.getConfig("db"))

    logger.info("Applying database migrations")
    applyDatabaseMigrations(databaseConfig)

    logger.info("Connecting to the database")
    implicit val xa = TransactorFactory(databaseConfig)

    val storage = globalConfig.getString("storage") match {
      case "in-memory" => new ObjectStorageService.InMemory()
      case "filesystem" => ObjectStorageService()
      case "s3" => {
        val s3Config = globalConfig.getConfig("s3")
        val bucket = s3Config.getString("bucket")
        val keyPrefix = s3Config.getString("keyPrefix")
        val region = if (s3Config.hasPath("region")) {
          Some(Region.of(s3Config.getString("region")))
        } else None
        new S3ObjectStorageService(bucket, keyPrefix, region)
      }
    }

    val objectManagementServicePromise: Promise[ObjectManagementService] = Promise()

    def onAtalaObject(notification: AtalaObjectNotification): Future[Unit] = {
      objectManagementServicePromise.future.map { objectManagementService =>
        objectManagementService.saveObject(notification)
        ()
      }
    }

    val keyValueService = new KeyValueService(new KeyValuesRepository(xa))

    val atalaReferenceLedger = globalConfig.getString("ledger") match {
      case "bitcoin" => initializeBitcoin(globalConfig.getConfig("bitcoin"), onAtalaObject)
      case "cardano" => initializeCardano(globalConfig.getConfig("cardano"), keyValueService, onAtalaObject)
      case "in-memory" =>
        logger.info("Using in-memory ledger")
        new InMemoryAtalaReferenceLedger(onAtalaObject)
    }

    logger.info("Creating blocks processor")
    val blockProcessingService = new BlockProcessingServiceImpl
    val didDataService = new DIDDataService(new DIDDataRepository(xa))
    val credentialsService = new CredentialsService(new CredentialsRepository(xa))
    val objectManagementService = new ObjectManagementService(storage, atalaReferenceLedger, blockProcessingService)
    objectManagementServicePromise.success(objectManagementService)

    val nodeService = new NodeServiceImpl(didDataService, objectManagementService, credentialsService)

    logger.info("Starting server")
    import io.grpc.protobuf.services.ProtoReflectionService
    server = ServerBuilder
      .forPort(NodeApp.port)
      .addService(NodeServiceGrpc.bindService(nodeService, executionContext))
      .addService(
        ProtoReflectionService.newInstance()
      ) //TODO: Decide before release if we should keep this (or guard it with a config flag)
      .build()
      .start()

    logger.info("Server started, listening on " + NodeApp.port)
    sys.addShutdownHook {
      System.err.println("*** shutting down gRPC server since JVM is shutting down")
      self.stop()
      System.err.println("*** server shut down")
    }
    ()
  }

  private def initializeBitcoin(config: Config, onAtalaObject: AtalaObjectNotificationHandler): AtalaService = {
    logger.info("Creating bitcoin client")

    val bitcoinNetwork = BitcoinNetwork.withNameInsensitive(config.getString("network"))
    val bitcoinClient = BitcoinClient(NodeConfig.bitcoinConfig(config))

    val atalaService = AtalaService(bitcoinNetwork, bitcoinClient, onAtalaObject)

    // TODO: Re-enable Bitcoin syncer
    /*
    val blocksRepository = new BlocksRepository(xa)
    val synchronizerConfig = SynchronizerConfig(30.seconds)
    val syncStatusService = new LedgerSynchronizationStatusService(bitcoinClient, blocksRepository)
    val synchronizerService =
      new LedgerSynchronizerService(bitcoinClient, blocksRepository, syncStatusService, atalaService)
    val task = new PollerSynchronizerTask(synchronizerConfig, bitcoinClient, synchronizerService)
     */

    atalaService
  }

  private def initializeCardano(
      config: Config,
      keyValueService: KeyValueService,
      onAtalaObject: AtalaObjectNotificationHandler
  ): CardanoLedgerService = {
    logger.info("Creating cardano client")
    CardanoLedgerService(NodeConfig.cardanoConfig(config), keyValueService, onAtalaObject)
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

  private def applyDatabaseMigrations(databaseConfig: TransactorFactory.Config): Unit = {
    val appliedMigrations = SchemaMigrations.migrate(databaseConfig)
    if (appliedMigrations == 0) {
      logger.info("Database up to date")
    } else {
      logger.info(s"$appliedMigrations migration scripts applied")
    }
  }
}
