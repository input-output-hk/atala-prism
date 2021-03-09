package io.iohk.atala.prism.management.console

import scala.concurrent.ExecutionContext
import cats.effect.{ExitCode, IO, IOApp, Resource}
import org.slf4j.LoggerFactory
import io.grpc.Server
import com.typesafe.config.ConfigFactory
import io.iohk.atala.prism.utils.GrpcUtils
import io.iohk.atala.prism.repositories.TransactorFactory
import io.iohk.atala.prism.config.NodeConfig
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeaderParser
import io.iohk.atala.prism.management.console.config.DefaultCredentialTypeConfig
import io.iohk.atala.prism.protos.node_api.NodeServiceGrpc
import io.iohk.atala.prism.protos.connector_api.{ConnectorServiceGrpc, ContactConnectionServiceGrpc}
import io.iohk.atala.prism.protos.console_api
import io.iohk.atala.prism.management.console.services._
import io.iohk.atala.prism.management.console.repositories._
import io.iohk.atala.prism.management.console.integrations.{
  ConnectionTokenServiceImpl,
  ContactsIntegrationService,
  ParticipantsIntegrationService
}

object ManagementConsoleApp extends IOApp {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val grpcConfig = GrpcUtils.GrpcConfig(port = 50054)

  implicit val ec = ExecutionContext.global

  override def run(args: List[String]): IO[ExitCode] = {
    // Cats resource runs in multiple threads, we have to pass class loader
    // explicit to provide a proper resource directory.
    val classLoader = Thread.currentThread().getContextClassLoader
    app(classLoader).use { grpcServer =>
      logger.info("Starting GRPC server")
      grpcServer.start()
      IO.never
    }
  }

  def app(classLoader: ClassLoader): Resource[IO, Server] = {
    for {
      // configs
      globalConfig <- Resource.liftF(IO.delay {
        logger.info("Loading config")
        ConfigFactory.load(classLoader)
      })
      defaultCredentialTypeConfig = DefaultCredentialTypeConfig(globalConfig)

      transactorConfig = TransactorFactory.transactorConfig(globalConfig)
      nodeConfig = NodeConfig(globalConfig)

      // db
      tx <- TransactorFactory.transactor[IO](transactorConfig)
      _ <- TransactorFactory.runDbMigrations[IO](tx, classLoader)

      // node
      node = GrpcUtils.createPlaintextStub(
        host = nodeConfig.host,
        port = nodeConfig.port,
        stub = NodeServiceGrpc.stub
      )

      // contact connection service
      connectorContactsService = GrpcUtils.createPlaintextStub(
        host = globalConfig.getConfig("connector").getString("host"),
        port = globalConfig.getConfig("connector").getInt("port"),
        stub = ContactConnectionServiceGrpc.stub
      )

      // contact contacts connection service
      connectorService = GrpcUtils.createPlaintextStub(
        host = globalConfig.getConfig("connector").getString("host"),
        port = globalConfig.getConfig("connector").getInt("port"),
        stub = ConnectorServiceGrpc.stub
      )

      // connection token service
      connectionTokenService = new ConnectionTokenServiceImpl(connectorService)

      // repositories
      contactsRepository = new ContactsRepository(tx)
      participantsRepository = new ParticipantsRepository(tx, defaultCredentialTypeConfig)
      requestNoncesRepository = new RequestNoncesRepository.PostgresImpl(tx)
      statisticsRepository = new StatisticsRepository(tx)
      credentialsRepository = new CredentialsRepository(tx)
      receivedCredentialsRepository = new ReceivedCredentialsRepository(tx)
      institutionGroupsRepository = new InstitutionGroupsRepository(tx)
      credentialIssuancesRepository = new CredentialIssuancesRepository(tx)

      authenticator = new ManagementConsoleAuthenticator(
        participantsRepository,
        requestNoncesRepository,
        node,
        GrpcAuthenticationHeaderParser
      )

      credentialsService = new CredentialsServiceImpl(
        credentialsRepository,
        authenticator,
        node
      )
      credentialsStoreService = new CredentialsStoreServiceImpl(receivedCredentialsRepository, authenticator)
      groupsService = new GroupsServiceImpl(institutionGroupsRepository, authenticator)
      participantsIntegrationService = new ParticipantsIntegrationService(participantsRepository, node)
      consoleService = new ConsoleServiceImpl(participantsIntegrationService, statisticsRepository, authenticator)
      contactsIntegrationService =
        new ContactsIntegrationService(contactsRepository, connectorContactsService, connectionTokenService)
      contactsService = new ContactsServiceImpl(contactsIntegrationService, authenticator)
      credentialIssuanceService = new CredentialIssuanceServiceImpl(
        credentialIssuancesRepository,
        authenticator
      )

      // gRPC server
      grpcServer <- GrpcUtils.createGrpcServer[IO](
        grpcConfig,
        sslConfigOption = None,
        console_api.ConsoleServiceGrpc.bindService(consoleService, ec),
        console_api.ContactsServiceGrpc.bindService(contactsService, ec),
        console_api.CredentialIssuanceServiceGrpc.bindService(credentialIssuanceService, ec),
        console_api.CredentialsServiceGrpc.bindService(credentialsService, ec),
        console_api.GroupsServiceGrpc.bindService(groupsService, ec),
        console_api.CredentialsStoreServiceGrpc.bindService(credentialsStoreService, ec),
        console_api.ConsoleServiceGrpc.bindService(consoleService, ec)
      )
    } yield grpcServer
  }

}
