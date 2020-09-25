package io.iohk.atala.prism.connector

import com.typesafe.config.{Config, ConfigFactory}
import io.grpc.{ManagedChannelBuilder, Server, ServerBuilder}
import io.iohk.atala.prism.admin.{AdminRepository, AdminServiceImpl}
import io.iohk.atala.prism.cmanager.grpc.services.{CredentialsServiceImpl, StudentsServiceImpl, SubjectsServiceImpl}
import io.iohk.atala.prism.cmanager.repositories.{CredentialsRepository, StudentsRepository}
import io.iohk.atala.prism.cstore.repositories.IndividualsRepository
import io.iohk.atala.prism.cviews.CredentialViewsService
import io.iohk.atala.prism.grpc.{GrpcAuthenticationHeaderParser, GrpcAuthenticatorInterceptor}
import io.iohk.atala.prism.intdemo.ConnectorIntegration.ConnectorIntegrationImpl
import io.iohk.atala.prism.intdemo._
import io.iohk.atala.prism.repositories.{SchemaMigrations, TransactorFactory}
import io.iohk.atala.prism.connector.payments.BraintreePayments
import io.iohk.atala.prism.connector.repositories._
import io.iohk.atala.prism.connector.services.{ConnectionsService, MessagesService, RegistrationService}
import io.iohk.atala.prism.console.repositories.{ContactsRepository, GroupsRepository, StoredCredentialsRepository}
import io.iohk.atala.prism.console.services.{ContactsServiceImpl, GroupsServiceImpl}
import io.iohk.atala.prism.cstore.services.CredentialsStoreService
import io.iohk.atala.prism.intdemo.protos.intdemo_api.{
  DegreeServiceGrpc,
  EmploymentServiceGrpc,
  IDServiceGrpc,
  InsuranceServiceGrpc
}
import io.iohk.prism.protos.admin_api.AdminServiceGrpc
import io.iohk.prism.protos.cmanager_api.{
  CredentialsServiceGrpc,
  GroupsServiceGrpc,
  StudentsServiceGrpc,
  SubjectsServiceGrpc
}
import io.iohk.prism.protos.connector_api
import io.iohk.prism.protos.console_api.ConsoleServiceGrpc
import io.iohk.prism.protos.cstore_api.CredentialsStoreServiceGrpc
import io.iohk.prism.protos.cviews_api.CredentialViewsServiceGrpc
import io.iohk.prism.protos.node_api.NodeServiceGrpc
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  * Run with `mill -i connector.run`, otherwise, the server will stay running even after ctrl+C.
  **/
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

    logger.info("Initializing Payment Wall")
    val braintreePayments =
      BraintreePayments(braintreePaymentsConfig(globalConfig.getConfig("braintreePayments")))(executionContext)

    // node client
    val configLoader = new ConfigLoader
    val nodeConfig = configLoader.nodeClientConfig(globalConfig.getConfig("node"))
    val nodeChannel = ManagedChannelBuilder.forAddress(nodeConfig.host, nodeConfig.port).usePlaintext().build()
    val node = NodeServiceGrpc.stub(nodeChannel)

    // connector repositories
    val connectionsRepository = new ConnectionsRepository.PostgresImpl(xa)(executionContext)
    val paymentsRepository = new PaymentsRepository(xa)(executionContext)
    val messagesRepository = new MessagesRepository(xa)(executionContext)
    val requestNoncesRepository = new RequestNoncesRepository.PostgresImpl(xa)(executionContext)
    val participantsRepository = new ParticipantsRepository(xa)(executionContext)

    // authenticator
    val authenticator = new SignedRequestsAuthenticator(
      participantsRepository,
      requestNoncesRepository,
      node,
      GrpcAuthenticationHeaderParser
    )

    // connector services
    val connectionsService =
      new ConnectionsService(connectionsRepository, paymentsRepository, braintreePayments, node)(executionContext)
    val messagesService = new MessagesService(messagesRepository)
    val registrationService = new RegistrationService(participantsRepository, node)(executionContext)
    val connectorService = new ConnectorService(
      connectionsService,
      messagesService,
      registrationService,
      braintreePayments,
      paymentsRepository,
      authenticator,
      node,
      participantsRepository
    )(
      executionContext
    )

    // cmanager
    val credentialsRepository = new CredentialsRepository(xa)(executionContext)
    val studentsRepository = new StudentsRepository(xa)(executionContext)
    val contactsRepository = new ContactsRepository(xa)(executionContext)
    val issuerGroupsRepository = new GroupsRepository(xa)(executionContext)
    val credentialsService =
      new CredentialsServiceImpl(credentialsRepository, contactsRepository, authenticator, node)(executionContext)
    val subjectsService = new SubjectsServiceImpl(contactsRepository, credentialsRepository, authenticator)(
      executionContext
    )
    val studentsService =
      new StudentsServiceImpl(studentsRepository, credentialsRepository, authenticator)(executionContext)
    val groupsService = new GroupsServiceImpl(issuerGroupsRepository, authenticator)(executionContext)

    val storeIndividualsService = new IndividualsRepository(xa)(executionContext)
    val storedCredentialsService = new StoredCredentialsRepository(xa)(executionContext)
    val credentialsStoreService =
      new CredentialsStoreService(storeIndividualsService, storedCredentialsService, contactsRepository, authenticator)(
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
    val consoleService = new ContactsServiceImpl(contactsRepository, authenticator)(executionContext)

    logger.info("Starting server")
    server = ServerBuilder
      .forPort(ConnectorApp.port)
      .intercept(new GrpcAuthenticatorInterceptor)
      .addService(connector_api.ConnectorServiceGrpc.bindService(connectorService, executionContext))
      .addService(CredentialsServiceGrpc.bindService(credentialsService, executionContext))
      .addService(SubjectsServiceGrpc.bindService(subjectsService, executionContext))
      .addService(StudentsServiceGrpc.bindService(studentsService, executionContext))
      .addService(GroupsServiceGrpc.bindService(groupsService, executionContext))
      .addService(CredentialsStoreServiceGrpc.bindService(credentialsStoreService, executionContext))
      .addService(CredentialViewsServiceGrpc.bindService(credentialViewsService, executionContext))
      .addService(IDServiceGrpc.bindService(idService, executionContext))
      .addService(DegreeServiceGrpc.bindService(degreeService, executionContext))
      .addService(EmploymentServiceGrpc.bindService(employmentService, executionContext))
      .addService(InsuranceServiceGrpc.bindService(insuranceService, executionContext))
      .addService(AdminServiceGrpc.bindService(adminService, executionContext))
      .addService(ConsoleServiceGrpc.bindService(consoleService, executionContext))
      .build()
      .start()

    logger.info("Server started, listening on " + ConnectorApp.port)
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

  private def braintreePaymentsConfig(config: Config): BraintreePayments.Config = {
    val publicKey = config.getString("publicKey")
    val privateKey = config.getString("privateKey")
    val merchantId = config.getString("merchantId")
    val tokenizationKey = config.getString("tokenizationKey")
    val production = config.getBoolean("production")
    BraintreePayments.Config(
      production = production,
      publicKey = publicKey,
      privateKey = privateKey,
      merchantId = merchantId,
      tokenizationKey = tokenizationKey
    )
  }
}
