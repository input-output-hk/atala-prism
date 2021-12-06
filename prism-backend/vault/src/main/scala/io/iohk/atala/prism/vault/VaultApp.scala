package io.iohk.atala.prism.vault

import cats.effect.{ExitCode, IO, IOApp, Resource}
import cats.effect.unsafe.IORuntime
import com.typesafe.config.{Config, ConfigFactory}
import doobie.hikari.HikariTransactor
import io.grpc.{ManagedChannelBuilder, Server, ServerBuilder}
import io.iohk.atala.prism.auth.AuthenticatorF
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.metrics.UptimeReporter
import io.iohk.atala.prism.protos.node_api.NodeServiceGrpc
import io.iohk.atala.prism.repositories.{SchemaMigrations, TransactorFactory}
import io.iohk.atala.prism.protos.vault_api
import io.iohk.atala.prism.vault.grpc.EncryptedDataVaultGrpcService
import io.iohk.atala.prism.vault.repositories.RecordsRepository
import io.iohk.atala.prism.vault.services.EncryptedDataVaultService
import kamon.Kamon
import kamon.module.Module
import org.slf4j.LoggerFactory
import tofu.logging.Logs

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

object VaultApp extends IOApp {

  private val port = 50054

  override def run(args: List[String]): IO[ExitCode] = {
    new VaultApp().start().use(_ => IO.never)
  }

}

class VaultApp() {
  self =>
  implicit val ec: ExecutionContextExecutor = ExecutionContext.global
  implicit val runtime: IORuntime = IORuntime.global

  private val logger = LoggerFactory.getLogger(this.getClass)

  private val vaultLogs: Logs[IO, IOWithTraceIdContext] =
    Logs.withContext[IO, IOWithTraceIdContext]

  private def start(): Resource[IO, Server] = {
    for {
      config <- loadConfig
      _ <- startMetrics(config)
      databaseConfig = TransactorFactory.transactorConfig(config)
      _ = applyDatabaseMigrations(databaseConfig)
      transactor <- connectToDB(databaseConfig)
      transactorWithIOContext = transactor.mapK(TraceId.liftToIOWithTraceId)
      node = createNodeClient(config)
      recordsRepository <- RecordsRepository.resource(transactorWithIOContext, vaultLogs)
      authenticator <- AuthenticatorF.resource(
        node,
        new VaultAuthenticator,
        vaultLogs
      )

      encryptedDataVaultService <- EncryptedDataVaultService.resource(recordsRepository, vaultLogs)
      encryptedDataVaultGrpcService = new EncryptedDataVaultGrpcService(encryptedDataVaultService, authenticator)(
        ec,
        runtime
      )
      server <- startServer(encryptedDataVaultGrpcService)
    } yield server
  }

  private def loadConfig: Resource[IO, Config] = Resource.pure[IO, Config] {
    logger.info("Loading config")
    ConfigFactory.load()
  }

  private def startMetrics(config: Config): Resource[IO, Module.Registration] = Resource.make(IO {
    logger.info("Setting-up uptime metrics")
    Kamon.init()
    Kamon.addReporter("uptime", new UptimeReporter(config))
  })(_ => IO.fromFuture(IO(Kamon.stop())))

  private def connectToDB(dbConfig: TransactorFactory.Config): Resource[IO, HikariTransactor[IO]] = {
    logger.info("Connecting to the database")
    TransactorFactory.transactor[IO](dbConfig)
  }

  private def createNodeClient(config: Config): NodeServiceGrpc.NodeServiceStub = {
    val nodeChannel = ManagedChannelBuilder
      .forAddress(
        config.getConfig("node").getString("host"),
        config.getConfig("node").getInt("port")
      )
      .usePlaintext()
      .build()
    NodeServiceGrpc.stub(nodeChannel)
  }

  private def startServer(encryptedDataVaultGrpcService: EncryptedDataVaultGrpcService): Resource[IO, Server] =
    Resource.make(IO {
      logger.info("Starting server")
      val server: Server = ServerBuilder
        .forPort(VaultApp.port)
        .addService(
          vault_api.EncryptedDataVaultServiceGrpc.bindService(encryptedDataVaultGrpcService, ec)
        )
        .build()
        .start()
      logger.info("Server is up and running")
      server
    })(server =>
      IO {
        logger.info("Shutting down the server")
        server.shutdown()
        server.awaitTermination()
        logger.info("Server termination completed")
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
