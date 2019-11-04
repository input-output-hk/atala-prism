package io.iohk.cvp.cmanager

import com.typesafe.config.{Config, ConfigFactory}
import io.grpc.protobuf.services.ProtoReflectionService
import io.grpc.{Server, ServerBuilder}
import io.iohk.cvp.cmanager.grpc.UserIdInterceptor
import io.iohk.cvp.cmanager.grpc.services.CredentialsServiceImpl
import io.iohk.cvp.cmanager.repositories.CredentialsRepository
import io.iohk.cvp.repositories.{SchemaMigrations, TransactorFactory}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext

/**
  * Run the app with: mill -i cmanager.run
  * Launch grpcui to interact with the server easily: grpcui -plaintext -import-path cmanager/protobuf -proto cmanager/protobuf/protos.proto localhost:50053
  */
object CManagerApp {
  def main(args: Array[String]): Unit = {
    val app = new CManagerApp(ExecutionContext.global)
    app.start()
    app.blockUntilShutdown()
  }
}

class CManagerApp(ec: ExecutionContext) { self =>
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val port = 50053

  private[this] var server: Server = null

  private def start(): Unit = {
    logger.info("Loading config")
    val globalConfig = ConfigFactory.load()
    val databaseConfig = transactorConfig(globalConfig.getConfig("db"))

    logger.info("Applying database migrations")
    applyDatabaseMigrations(databaseConfig)

    logger.info("Connecting to the database")
    val xa = TransactorFactory(databaseConfig)

    val credentialsRepository = new CredentialsRepository(xa)(ec)
    val credentialsService = new CredentialsServiceImpl(credentialsRepository)(ec)

    logger.info("Starting server")
    server = ServerBuilder
      .forPort(port)
      .intercept(new UserIdInterceptor)
      .addService(ProtoReflectionService.newInstance())
      .addService(protos.CredentialsServiceGrpc.bindService(credentialsService, ec))
      .build()
      .start()

    logger.info("Server started, listening on " + port)
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
