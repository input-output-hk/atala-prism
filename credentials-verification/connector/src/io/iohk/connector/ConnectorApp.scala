package io.iohk.connector

import com.typesafe.config.{Config, ConfigFactory}
import io.grpc.{Server, ServerBuilder}
import io.iohk.connector.protos._
import io.iohk.connector.repositories.ConnectionsRepository
import io.iohk.connector.services.ConnectionsService
import io.iohk.cvp.repositories.{SchemaMigrations, TransactorFactory}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext

/**
  * Run with `mill -i connector.run`, otherwise, the server will stay running even after ctrl+C.
  */
object ConnectorApp {
  def main(args: Array[String]): Unit = {
    val server = new ConnectorApp(ExecutionContext.global)
    server.start()
    server.blockUntilShutdown()
  }

  private val port = 50051
}

class ConnectorApp(executionContext: ExecutionContext) { self =>
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

    val connectionsRepository = new ConnectionsRepository(xa)(executionContext)
    val connectionsService = new ConnectionsService(connectionsRepository)
    val connectorService = new ConnectorService(connectionsService)(executionContext)

    logger.info("Starting server")
    server = ServerBuilder
      .forPort(ConnectorApp.port)
      .intercept(new UserIdInterceptor)
      .addService(ConnectorServiceGrpc.bindService(connectorService, executionContext))
      .build()
      .start()

    logger.info("Server started, listening on " + ConnectorApp.port)
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
}
