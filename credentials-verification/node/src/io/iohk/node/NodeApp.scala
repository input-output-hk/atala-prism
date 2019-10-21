package io.iohk.node

import com.typesafe.config.{Config, ConfigFactory}
import io.iohk.cvp.repositories.{SchemaMigrations, TransactorFactory}
import io.iohk.node.bitcoin.BitcoinClient
import io.iohk.node.repositories.blocks.BlocksRepository
import io.iohk.node.synchronizer.{
  LedgerSynchronizationStatusService,
  LedgerSynchronizerService,
  PollerSynchronizerTask,
  SynchronizerConfig
}
import monix.execution.Scheduler.Implicits.{global => scheduler}
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import io.grpc.{Server, ServerBuilder}
import io.iohk.node.geud_node._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import io.iohk.node.services.AtalaService
import io.iohk.node.objects.ObjectStorageService

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

class NodeApi(atalaService: AtalaService)(implicit executionContext: ExecutionContext)
    extends NodeServiceGrpc.NodeService { self =>

  import io.iohk.node.atala_bitcoin._

  def publishDidDocument(request: PublishDidDocumentRequest): Future[PublishDidDocumentResponse] = {
    val txDefinition = AtalaTx.Definition.PublishDidDocument(request)
    val tx = AtalaTx(txDefinition)
    atalaService
      .publishAtalaTransaction(tx)
      .value
      .map {
        case Right(_) =>
          PublishDidDocumentResponse()
        case Left(left) =>
          // TODO: Decide on a correct representation of errors
          throw new Exception("Unexpected error trying to publish an atala transaction\n" + left)
      }

  }

  def getDidDocument(request: GetDidDocumentRequest): Future[GetDidDocumentResponse] = ???
  def getProofOfCredentialIssued(
      request: GetProofOfCredentialIssuedRequest
  ): Future[GetProofOfCredentialIssuedResponse] = ???
  def publishProofOfCredentialIssued(
      request: PublishProofOfCredentialIssuedRequest
  ): Future[PublishProofOfCredentialIssuedResponse] = ???
  def revokeProofOfCredentialIssued(
      request: RevokeProofOfCredentialIssuedRequest
  ): Future[RevokeProofOfCredentialIssuedResponse] = ???

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
    val xa = TransactorFactory(databaseConfig)
    val blocksRepository = new BlocksRepository(xa)

    logger.info("Creating bitcoin client")
    val bitcoinClient = BitcoinClient(bitcoinConfig(globalConfig.getConfig("bitcoin")))
    val storage = ObjectStorageService()

    val synchronizerConfig = SynchronizerConfig(30.seconds)
    val syncStatusService = new LedgerSynchronizationStatusService(bitcoinClient, blocksRepository)
    val synchronizerService = new LedgerSynchronizerService(bitcoinClient, blocksRepository, syncStatusService)
    val task = new PollerSynchronizerTask(synchronizerConfig, bitcoinClient, synchronizerService)

    val atalaService = AtalaService(bitcoinClient, storage)
    val nodeApi = new NodeApi(atalaService)

    logger.info("Starting server")
    import io.grpc.protobuf.services.ProtoReflectionService
    server = ServerBuilder
      .forPort(NodeApp.port)
      .addService(NodeServiceGrpc.bindService(nodeApi, executionContext))
      .addService(ProtoReflectionService.newInstance())
      .build()
      .start()

    logger.info("Server started, listening on " + NodeApp.port)
    sys.addShutdownHook {
      System.err.println("*** shutting down gRPC server since JVM is shutting down")
      self.stop()
      System.err.println("*** server shut down")
    }
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
