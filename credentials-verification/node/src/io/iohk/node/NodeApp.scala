package io.iohk.node

import cats.effect.IO
import com.typesafe.config.{Config, ConfigFactory}
import doobie.util.transactor.Transactor
import io.grpc.{Server, ServerBuilder}
import io.iohk.cvp.repositories.{SchemaMigrations, TransactorFactory}
import io.iohk.node.bitcoin.BitcoinClient
import io.iohk.node.models.SHA256Digest
import io.iohk.node.objects.ObjectStorageService
import io.iohk.node.repositories.DIDDataRepository
import io.iohk.node.repositories.atalaobjects.AtalaObjectsRepository
import io.iohk.node.repositories.blocks.BlocksRepository
import io.iohk.node.services.{AtalaService, BlockProcessingServiceImpl, DIDDataService, ObjectManagementService}
import io.iohk.node.synchronizer.{LedgerSynchronizationStatusService, LedgerSynchronizerService, SynchronizerConfig}
import io.iohk.nodenew.geud_node_new._
import monix.execution.Scheduler.Implicits.{global => scheduler}
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}

/**
  * Run with `mill -i node.run`, otherwise, the server will stay running even after ctrl+C.
  */
object NodeApp {

  private val logger = LoggerFactory.getLogger(this.getClass)

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
    val databaseConfig = transactorConfig(globalConfig.getConfig("db"))

    logger.info("Applying database migrations")
    applyDatabaseMigrations(databaseConfig)

    logger.info("Connecting to the database")
    implicit val xa = TransactorFactory(databaseConfig)
    val atalaObjectsRepository = new AtalaObjectsRepository(xa)

    val storage = ObjectStorageService()

    val objectManagementServicePromise: Promise[ObjectManagementService] = Promise()
    def onAtalaReference(ref: SHA256Digest): Future[Unit] = {
      objectManagementServicePromise.future.map { objectManagementService =>
        objectManagementService.saveReference(ref)
      }
    }

    val atalaReferenceLedger = globalConfig.getString("ledger") match {
      case "bitcoin" => initializeBitcoin(globalConfig.getConfig("bitcoin"), onAtalaReference)
      case "in-memory" =>
        logger.info("Using in-memory ledger")
        new InMemoryAtalaReferenceLedger(onAtalaReference)
    }

    logger.info("Creating blocks processor")
    val blockProcessingService = new BlockProcessingServiceImpl
    val didDataService = new DIDDataService(new DIDDataRepository(xa))
    val objectManagementService = new ObjectManagementService(storage, atalaReferenceLedger, blockProcessingService)
    objectManagementServicePromise.success(objectManagementService)

    val nodeService = new NodeServiceImpl(didDataService, objectManagementService)

    logger.info("Starting server")
    import io.grpc.protobuf.services.ProtoReflectionService
    server = ServerBuilder
      .forPort(NodeApp.port)
      .addService(NodeServiceGrpc.bindService(nodeService, executionContext))
      .addService(ProtoReflectionService.newInstance()) //TODO: Decide before release if we should keep this (or guard it with a config flag)
      .build()
      .start()

    logger.info("Server started, listening on " + NodeApp.port)
    sys.addShutdownHook {
      System.err.println("*** shutting down gRPC server since JVM is shutting down")
      self.stop()
      System.err.println("*** server shut down")
    }
  }

  def initializeBitcoin(config: Config, onAtalaReference: SHA256Digest => Future[Unit])(
      implicit xa: Transactor[IO]
  ): AtalaService = {
    logger.info("Creating bitcoin client")
    val bitcoinClient = BitcoinClient(bitcoinConfig(config))

    val blocksRepository = new BlocksRepository(xa)

    val atalaService = AtalaService(bitcoinClient, onAtalaReference)

    val synchronizerConfig = SynchronizerConfig(30.seconds)
    val syncStatusService = new LedgerSynchronizationStatusService(bitcoinClient, blocksRepository)
    val synchronizerService =
      new LedgerSynchronizerService(bitcoinClient, blocksRepository, syncStatusService, atalaService)
    //val task = new PollerSynchronizerTask(synchronizerConfig, bitcoinClient, synchronizerService)

    atalaService
  }

  private def stop(): Unit = {
    if (server != null) {
      server.shutdown()
    }
  }

  private def blockUntilShutdown(): Unit = {
    if (server != null) {
      server.awaitTermination()
    }
  }

  def applyDatabaseMigrations(databaseConfig: TransactorFactory.Config): Unit = {
    val appliedMigrations = SchemaMigrations.migrate(databaseConfig)
    if (appliedMigrations == 0) {
      logger.info("Database up to date")
    } else {
      logger.info(s"$appliedMigrations migration scripts applied")
    }
  }

  def transactorConfig(config: Config): TransactorFactory.Config = {
    val url = config.getString("url")
    val username = config.getString("username")
    val password = config.getString("password")
    TransactorFactory.Config(
      jdbcUrl = url,
      username = username,
      password = password
    )
  }

  def bitcoinConfig(config: Config): BitcoinClient.Config = {
    val host = config.getString("host")
    val port = config.getInt("port")
    val username = config.getString("username")
    val password = config.getString("password")
    BitcoinClient.Config(host, port, username, password)
  }

}
