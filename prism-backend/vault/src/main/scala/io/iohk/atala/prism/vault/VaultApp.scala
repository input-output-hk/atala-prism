package io.iohk.atala.prism.vault

import cats.effect.{IO, ContextShift}
import com.typesafe.config.ConfigFactory
import io.grpc.{ManagedChannelBuilder, Server, ServerBuilder}
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeaderParser
import io.iohk.atala.prism.protos.node_api.NodeServiceGrpc
import io.iohk.atala.prism.repositories.{SchemaMigrations, TransactorFactory}
import io.iohk.atala.prism.protos.vault_api
import io.iohk.atala.prism.vault.repositories.{PayloadsRepository, RequestNoncesRepository}
import io.iohk.atala.prism.vault.services.EncryptedDataVaultServiceImpl
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext

object VaultApp {
  def main(args: Array[String]): Unit = {
    val server = new VaultApp(ExecutionContext.global)
    server.start()
    server.blockUntilShutdown()
    server.releaseResources()
  }

  private val port = 50054
}

class VaultApp(executionContext: ExecutionContext) {
  self =>
  private val logger = LoggerFactory.getLogger(this.getClass)

  private[this] var server: Server = null
  private[this] var releaseTransactor: Option[IO[Unit]] = None

  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  private def start(): Unit = {
    logger.info("Loading config")
    val globalConfig = ConfigFactory.load()
    val databaseConfig = TransactorFactory.transactorConfig(globalConfig)

    logger.info("Applying database migrations")
    applyDatabaseMigrations(databaseConfig)

    logger.info("Connecting to the database")
    val (transactor, releaseTransactor) = TransactorFactory.transactor[IO](databaseConfig).allocated.unsafeRunSync()
    self.releaseTransactor = Some(releaseTransactor)

    // Node client
    val nodeChannel = ManagedChannelBuilder
      .forAddress(
        globalConfig.getConfig("node").getString("host"),
        globalConfig.getConfig("node").getInt("port")
      )
      .usePlaintext()
      .build()
    val node = NodeServiceGrpc.stub(nodeChannel)

    // Vault repositories
    val payloadsRepository = new PayloadsRepository(transactor)(executionContext)
    val requestNoncesRepository = new RequestNoncesRepository.PostgresImpl(transactor)(executionContext)

    val authenticator = new VaultAuthenticator(
      requestNoncesRepository,
      node,
      GrpcAuthenticationHeaderParser
    )

    val encryptedDataVaultService = new EncryptedDataVaultServiceImpl(payloadsRepository, authenticator)(
      executionContext
    )

    logger.info("Starting server")
    server = ServerBuilder
      .forPort(VaultApp.port)
      .addService(vault_api.EncryptedDataVaultServiceGrpc.bindService(encryptedDataVaultService, executionContext))
      .build()
      .start()
  }

  private def blockUntilShutdown(): Unit = {
    if (server != null) {
      server.awaitTermination()
    }
  }

  private def releaseResources(): Unit = releaseTransactor.foreach(_.unsafeRunSync())

  private def applyDatabaseMigrations(databaseConfig: TransactorFactory.Config): Unit = {
    val appliedMigrations = SchemaMigrations.migrate(databaseConfig)
    if (appliedMigrations == 0) {
      logger.info("Database up to date")
    } else {
      logger.info(s"$appliedMigrations migration scripts applied")
    }
  }
}
