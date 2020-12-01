package io.iohk.atala.prism.management.console

import cats.effect.IO
import com.typesafe.config.ConfigFactory
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
    server.releaseResources()
  }

  private val port = 50054
}

class ManagementConsoleApp(executionContext: ExecutionContext) {
  self =>
  private val logger = LoggerFactory.getLogger(this.getClass)

  private[this] var server: Server = null
  private[this] var releaseTransactor: Option[IO[Unit]] = None

  private def start(): Unit = {
    logger.info("Loading config")
    val globalConfig = ConfigFactory.load()
    val databaseConfig = TransactorFactory.transactorConfig(globalConfig)

    logger.info("Applying database migrations")
    applyDatabaseMigrations(databaseConfig)

    logger.info("Connecting to the database")
    val (transactor, releaseTransactor) = TransactorFactory.transactorIO(databaseConfig).allocated.unsafeRunSync()
    self.releaseTransactor = Some(releaseTransactor)

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
    val contactsRepository = new ContactsRepository(transactor)(executionContext)
    val participantsRepository = new ParticipantsRepository(transactor)(executionContext)
    val requestNoncesRepository = new RequestNoncesRepository.PostgresImpl(transactor)(executionContext)
    val statisticsRepository = new StatisticsRepository(transactor)
    val credentialsRepository = new CredentialsRepository(transactor)(executionContext)
    val receivedCredentialsRepository = new ReceivedCredentialsRepository(transactor)(executionContext)
    val institutionGroupsRepository = new InstitutionGroupsRepository(transactor)(executionContext)

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
