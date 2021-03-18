package io.iohk.atala.prism.connector

import cats.effect.{ContextShift, IO}
import com.typesafe.config.ConfigFactory
import io.grpc.{ManagedChannelBuilder, Server, ServerBuilder}
import io.iohk.atala.prism.admin.{AdminRepository, AdminServiceImpl}
import io.iohk.atala.prism.auth.grpc.{GrpcAuthenticationHeaderParser, GrpcAuthenticatorInterceptor}
import io.iohk.atala.prism.connector.repositories._
import io.iohk.atala.prism.connector.services._
import io.iohk.atala.prism.console.integrations.CredentialsIntegrationService
import io.iohk.atala.prism.console.repositories._
import io.iohk.atala.prism.console.services._
import io.iohk.atala.prism.cviews.CredentialViewsService
import io.iohk.atala.prism.intdemo.ConnectorIntegration.ConnectorIntegrationImpl
import io.iohk.atala.prism.intdemo._
import io.iohk.atala.prism.intdemo.protos.intdemo_api.{
  DegreeServiceGrpc,
  EmploymentServiceGrpc,
  IDServiceGrpc,
  InsuranceServiceGrpc
}
import io.iohk.atala.prism.protos.admin_api.AdminServiceGrpc
import io.iohk.atala.prism.protos.connector_api
import io.iohk.atala.prism.protos.connector_api.ContactConnectionServiceGrpc
import io.iohk.atala.prism.protos.console_api._
import io.iohk.atala.prism.protos.cviews_api.CredentialViewsServiceGrpc
import io.iohk.atala.prism.protos.node_api.NodeServiceGrpc
import io.iohk.atala.prism.repositories.{SchemaMigrations, TransactorFactory}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Try

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

  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  private[this] var server: Server = null
  private[this] var messageNotificationService: MessageNotificationService = null

  private def start(): Unit = {
    logger.info("Loading config")
    val globalConfig = ConfigFactory.load()
    val databaseConfig = TransactorFactory.transactorConfig(globalConfig)

    logger.info("Applying database migrations")
    applyDatabaseMigrations(databaseConfig)

    logger.info("Connecting to the database")
    val (xa, releaseXa) = TransactorFactory.transactor[IO](databaseConfig).allocated.unsafeRunSync()

    logger.info("Loading DID whitelist")
    val didWhitelist = DidWhitelistLoader.load(globalConfig)
    if (didWhitelist.isEmpty) {
      logger.warn(s"DID whitelist is empty, which prevents integrating the console backend")
    } else {
      logger.info(s"DID whitelist:\n${didWhitelist.map(_.value).map("- " + _).mkString("\n")}")
    }

    // node client
    val configLoader = new ConfigLoader
    val nodeConfig = configLoader.nodeClientConfig(globalConfig.getConfig("node"))
    val nodeChannel = ManagedChannelBuilder.forAddress(nodeConfig.host, nodeConfig.port).usePlaintext().build()
    val node = NodeServiceGrpc.stub(nodeChannel)

    // connector repositories
    val connectionsRepository = new ConnectionsRepository.PostgresImpl(xa)(executionContext)
    val messagesRepository = new MessagesRepository(xa)(executionContext)
    val requestNoncesRepository = new RequestNoncesRepository.PostgresImpl(xa)(executionContext)
    val participantsRepository = new ParticipantsRepository(xa)

    // authenticator
    val authenticator = new ConnectorAuthenticator(
      participantsRepository,
      requestNoncesRepository,
      node,
      GrpcAuthenticationHeaderParser
    )

    // Background services
    val contextShift = IO.contextShift(executionContext)
    val timer = IO.timer(executionContext)
    messageNotificationService = MessageNotificationService(xa)(contextShift, timer)
    messageNotificationService.start()

    // connector services
    val connectionsService = new ConnectionsService(connectionsRepository, node)(executionContext)
    val messagesService = new MessagesService(messagesRepository)
    val registrationService = new RegistrationService(participantsRepository, node)(executionContext)
    val contactConnectionService = new ContactConnectionService(connectionsService, authenticator, didWhitelist)(
      executionContext
    )
    val connectorService = new ConnectorService(
      connectionsService,
      messagesService,
      registrationService,
      messageNotificationService,
      authenticator,
      node,
      participantsRepository
    )(
      executionContext
    )

    // cmanager
    val statisticsRepository = new StatisticsRepository(xa)
    val credentialsRepository = new CredentialsRepository(xa)(executionContext)
    val contactsRepository = new ContactsRepository(xa)(executionContext)
    val issuerGroupsRepository = new GroupsRepository(xa)(executionContext)
    val credentialsIntegration = new CredentialsIntegrationService(credentialsRepository, node)(executionContext)
    val credentialsService = new CredentialsServiceImpl(
      credentialsRepository,
      contactsRepository,
      credentialsIntegration,
      authenticator,
      node
    )(executionContext)
    val groupsService = new GroupsServiceImpl(issuerGroupsRepository, authenticator)(executionContext)

    val storedCredentialsService = new StoredCredentialsRepository(xa)(executionContext)
    val credentialsStoreService =
      new CredentialsStoreService(storedCredentialsService, authenticator)(
        executionContext
      )
    val credentialViewsService = new CredentialViewsService(authenticator)(executionContext)

    // admin
    val adminRepository = new AdminRepository(xa)(executionContext)
    val adminService = new AdminServiceImpl(adminRepository)(executionContext)

    // interactive demo services
    val intDemoRepository = new IntDemoRepository(xa)
    val connectorIntegration = new ConnectorIntegrationImpl(connectionsService, messagesService)(executionContext)
    val idService =
      new IdServiceImpl(connectorIntegration, intDemoRepository, schedulerPeriod = 1.second)(executionContext)
    val degreeService =
      new DegreeServiceImpl(connectorIntegration, intDemoRepository, schedulerPeriod = 1.second)(executionContext)
    val employmentService =
      new EmploymentServiceImpl(connectorIntegration, intDemoRepository, schedulerPeriod = 1.second)(executionContext)
    val insuranceService =
      new InsuranceServiceImpl(connectorIntegration, intDemoRepository, schedulerPeriod = 1.second)(executionContext)

    // console (unified backend) services
    val consoleService = new ConsoleServiceImpl(statisticsRepository, authenticator)(
      executionContext
    )
    val contactsService = new ContactsServiceImpl(contactsRepository, authenticator)(
      executionContext
    )

    logger.info("Starting server")
    server = ServerBuilder
      .forPort(ConnectorApp.port)
      .intercept(new GrpcAuthenticatorInterceptor)
      .addService(connector_api.ConnectorServiceGrpc.bindService(connectorService, executionContext))
      .addService(CredentialsServiceGrpc.bindService(credentialsService, executionContext))
      .addService(GroupsServiceGrpc.bindService(groupsService, executionContext))
      .addService(CredentialsStoreServiceGrpc.bindService(credentialsStoreService, executionContext))
      .addService(CredentialViewsServiceGrpc.bindService(credentialViewsService, executionContext))
      .addService(IDServiceGrpc.bindService(idService, executionContext))
      .addService(DegreeServiceGrpc.bindService(degreeService, executionContext))
      .addService(EmploymentServiceGrpc.bindService(employmentService, executionContext))
      .addService(InsuranceServiceGrpc.bindService(insuranceService, executionContext))
      .addService(AdminServiceGrpc.bindService(adminService, executionContext))
      .addService(ConsoleServiceGrpc.bindService(consoleService, executionContext))
      .addService(ContactsServiceGrpc.bindService(contactsService, executionContext))
      .addService(ContactConnectionServiceGrpc.bindService(contactConnectionService, executionContext))
      .build()
      .start()

    logger.info("Server started, listening on " + ConnectorApp.port)
    sys.addShutdownHook {
      System.err.println("*** shutting down gRPC server since JVM is shutting down")
      releaseXa.unsafeRunSync()
      self.stop()
      System.err.println("*** server shut down")
    }
    ()
  }

  private def stop(): Unit = {
    if (messageNotificationService != null) {
      Try(messageNotificationService.stop())
    }
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

  def applyDatabaseMigrations(databaseConfig: TransactorFactory.Config): Unit = {
    val appliedMigrations = SchemaMigrations.migrate(databaseConfig)
    if (appliedMigrations == 0) {
      logger.info("Database up to date")
    } else {
      logger.info(s"$appliedMigrations migration scripts applied")
    }
  }
}
