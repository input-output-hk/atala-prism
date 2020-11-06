package io.iohk.atala.prism.management.console

import com.typesafe.config.{Config, ConfigFactory}
import io.grpc.{Server, ServerBuilder}
import io.iohk.atala.prism.management.console.repositories.ContactsRepository
import io.iohk.atala.prism.management.console.services.ConsoleServiceImpl
import io.iohk.atala.prism.repositories.{SchemaMigrations, TransactorFactory}
import io.iohk.atala.prism.protos.console_api
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext

object ManagementConsoleApp {
  def main(args: Array[String]): Unit = {
    val server = new ManagementConsoleApp(ExecutionContext.global)
    server.start()
    server.blockUntilShutdown()
  }

  private val port = 50054
}

class ManagementConsoleApp(executionContext: ExecutionContext) {
  self =>
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

    // Vault repositories
    val contactsRepository = new ContactsRepository(xa)(executionContext)

    val consoleService = new ConsoleServiceImpl(contactsRepository)(
      executionContext
    )

    logger.info("Starting server")
    server = ServerBuilder
      .forPort(ManagementConsoleApp.port)
      .addService(console_api.ConsoleServiceGrpc.bindService(consoleService, executionContext))
      .build()
      .start()
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

  private def transactorConfig(config: Config): TransactorFactory.Config = {
    val url = config.getString("url")
    val username = config.getString("username")
    val password = config.getString("password")
    TransactorFactory.Config(
      jdbcUrl = url,
      username = username,
      password = password
    )
  }
}
