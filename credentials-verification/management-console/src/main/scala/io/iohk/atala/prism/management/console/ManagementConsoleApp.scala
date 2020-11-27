package io.iohk.atala.prism.management.console

import com.typesafe.config.{Config, ConfigFactory}
import io.grpc.{ManagedChannelBuilder, Server, ServerBuilder}
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeaderParser
import io.iohk.atala.prism.management.console.repositories.{
  ContactsRepository,
  CredentialsRepository,
  InstitutionGroupsRepository,
  ParticipantsRepository,
  ReceivedCredentialsRepository,
  RequestNoncesRepository,
  StatisticsRepository
}
import io.iohk.atala.prism.management.console.services.{
  ConsoleServiceImpl,
  CredentialsServiceImpl,
  CredentialsStoreServiceImpl,
  GroupsServiceImpl
}
import io.iohk.atala.prism.repositories.{SchemaMigrations, TransactorFactory}
import io.iohk.atala.prism.protos.{cmanager_api, console_api, cstore_api}
import io.iohk.atala.prism.protos.node_api.NodeServiceGrpc
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

    // node client
    val nodeChannel = ManagedChannelBuilder
      .forAddress(
        globalConfig.getConfig("node").getString("host"),
        globalConfig.getConfig("node").getInt("port")
      )
      .usePlaintext()
      .build()
    val node = NodeServiceGrpc.stub(nodeChannel)

    // Vault repositories
    val contactsRepository = new ContactsRepository(xa)(executionContext)
    val participantsRepository = new ParticipantsRepository(xa)(executionContext)
    val requestNoncesRepository = new RequestNoncesRepository.PostgresImpl(xa)(executionContext)
    val statisticsRepository = new StatisticsRepository(xa)
    val credentialsRepository = new CredentialsRepository(xa)(executionContext)
    val receivedCredentialsRepository = new ReceivedCredentialsRepository(xa)(executionContext)
    val institutionGroupsRepository = new InstitutionGroupsRepository(xa)(executionContext)

    val authenticator = new ManagementConsoleAuthenticator(
      participantsRepository,
      requestNoncesRepository,
      node,
      GrpcAuthenticationHeaderParser
    )

    val credentialsService =
      new CredentialsServiceImpl(credentialsRepository, contactsRepository, authenticator, node)(executionContext)
    val credentialsStoreService =
      new CredentialsStoreServiceImpl(receivedCredentialsRepository, authenticator)(executionContext)
    val groupsService = new GroupsServiceImpl(institutionGroupsRepository, authenticator)(executionContext)
    val consoleService = new ConsoleServiceImpl(contactsRepository, statisticsRepository, authenticator)(
      executionContext
    )

    logger.info("Starting server")
    server = ServerBuilder
      .forPort(ManagementConsoleApp.port)
      .addService(console_api.ConsoleServiceGrpc.bindService(consoleService, executionContext))
      .addService(cmanager_api.CredentialsServiceGrpc.bindService(credentialsService, executionContext))
      .addService(cmanager_api.GroupsServiceGrpc.bindService(groupsService, executionContext))
      .addService(cstore_api.CredentialsStoreServiceGrpc.bindService(credentialsStoreService, executionContext))
      .addService(console_api.ConsoleServiceGrpc.bindService(consoleService, executionContext))
      .build()
      .start()

    logger.info("Server started, listening on " + ManagementConsoleApp.port)
    sys.addShutdownHook {
      System.err.println("*** shutting down gRPC server since JVM is shutting down")
      self.stop()
      System.err.println("*** server shut down")
    }
    ()
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
