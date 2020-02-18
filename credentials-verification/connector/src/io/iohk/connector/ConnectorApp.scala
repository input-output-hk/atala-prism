package io.iohk.connector

import com.typesafe.config.{Config, ConfigFactory}
import io.grpc.{ManagedChannelBuilder, Server, ServerBuilder}
import io.iohk.connector.payments.BraintreePayments
import io.iohk.connector.repositories.{ConnectionsRepository, MessagesRepository, PaymentsRepository}
import io.iohk.connector.services.{ConnectionsService, MessagesService}
import io.iohk.cvp.admin.protos.AdminServiceGrpc
import io.iohk.cvp.admin.{AdminRepository, AdminServiceImpl}
import io.iohk.cvp.cmanager.grpc.services.{CredentialsServiceImpl, GroupsServiceImpl, StudentsServiceImpl}
import io.iohk.cvp.cmanager.protos.{CredentialsServiceGrpc, GroupsServiceGrpc, StudentsServiceGrpc}
import io.iohk.cvp.cmanager.repositories.{
  CredentialsRepository,
  IssuerGroupsRepository,
  IssuersRepository,
  StudentsRepository
}
import io.iohk.cvp.connector.protos._
import io.iohk.cvp.cstore.CredentialsStoreService
import io.iohk.cvp.cstore.protos.CredentialsStoreServiceGrpc
import io.iohk.cvp.cstore.services.{StoreIndividualsService, StoreUsersService, StoredCredentialsService}
import io.iohk.cvp.grpc.GrpcAuthenticatorInterceptor
import io.iohk.cvp.repositories.{SchemaMigrations, TransactorFactory}
import io.iohk.nodenew.node_api.NodeServiceGrpc
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext

/**
  * Run with `mill -i connector.run`, otherwise, the server will stay running even after ctrl+C.
  *
  * Launch grpcui with: grpcui -plaintext -import-path connector/protobuf --proto connector/protobuf/cmanager/protos.proto --proto connector/protobuf/connector/protos.proto localhost:50051
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

    // authenticator
    val authenticator = new SignedRequestsAuthenticator(connectionsRepository)

    // connector services
    val connectionsService =
      new ConnectionsService(connectionsRepository, paymentsRepository, braintreePayments)(executionContext)
    val messagesService = new MessagesService(messagesRepository)
    val connectorService =
      new ConnectorService(connectionsService, messagesService, braintreePayments, paymentsRepository, authenticator)(
        executionContext
      )

    // cmanager
    val issuersRepository = new IssuersRepository(xa)(executionContext)
    val credentialsRepository = new CredentialsRepository(xa)(executionContext)
    val studentsRepository = new StudentsRepository(xa)(executionContext)
    val issuerGroupsRepository = new IssuerGroupsRepository(xa)(executionContext)
    val credentialsService =
      new CredentialsServiceImpl(issuersRepository, credentialsRepository, authenticator)(executionContext)
    val studentsService =
      new StudentsServiceImpl(studentsRepository, credentialsRepository, authenticator)(executionContext)
    val groupsService = new GroupsServiceImpl(issuerGroupsRepository, authenticator)(executionContext)

    val storeUsersService = new StoreUsersService(xa)(executionContext)
    val storeIndividualsService = new StoreIndividualsService(xa)(executionContext)
    val storedCredentialsService = new StoredCredentialsService(xa)(executionContext)
    val credentialsStoreService =
      new CredentialsStoreService(storeUsersService, storeIndividualsService, storedCredentialsService, authenticator)(
        executionContext
      )

    // admin
    val adminRepository = new AdminRepository(xa)(executionContext)
    val adminService = new AdminServiceImpl(adminRepository)(executionContext)

    //
    logger.info("Starting server")
    server = ServerBuilder
      .forPort(ConnectorApp.port)
      .intercept(new GrpcAuthenticatorInterceptor)
      .addService(ConnectorServiceGrpc.bindService(connectorService, executionContext))
      .addService(CredentialsServiceGrpc.bindService(credentialsService, executionContext))
      .addService(StudentsServiceGrpc.bindService(studentsService, executionContext))
      .addService(GroupsServiceGrpc.bindService(groupsService, executionContext))
      .addService(CredentialsStoreServiceGrpc.bindService(credentialsStoreService, executionContext))
      .addService(AdminServiceGrpc.bindService(adminService, executionContext))
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
