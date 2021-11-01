package io.iohk.atala.prism.management.console

import cats.effect.{ExitCode, IO, IOApp, Resource}
import com.typesafe.config.ConfigFactory
import io.grpc.Server
import io.iohk.atala.prism.auth.grpc.{GrpcAuthenticationHeaderParser, GrpcAuthenticatorInterceptor}
import io.iohk.atala.prism.config.NodeConfig
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.management.console.clients.ConnectorClient
import io.iohk.atala.prism.management.console.config.DefaultCredentialTypeConfig
import io.iohk.atala.prism.management.console.grpc.GroupsGrpcService
import io.iohk.atala.prism.management.console.grpc.CredentialTypesGrpcService
import io.iohk.atala.prism.management.console.grpc.ContactsGrpcService
import io.iohk.atala.prism.management.console.grpc.CredentialsStoreGrpcService
import io.iohk.atala.prism.management.console.grpc.CredentialIssuanceGrpcService
import io.iohk.atala.prism.management.console.grpc.CredentialsGrpcService
import io.iohk.atala.prism.management.console.grpc.ConsoleGrpcService
import io.iohk.atala.prism.management.console.integrations.{
  ContactsIntegrationService,
  CredentialsIntegrationService,
  ParticipantsIntegrationService
}
import io.iohk.atala.prism.management.console.repositories._
import io.iohk.atala.prism.management.console.services._
import io.iohk.atala.prism.metrics.UptimeReporter
import io.iohk.atala.prism.protos.console_api
import io.iohk.atala.prism.protos.node_api.NodeServiceGrpc
import io.iohk.atala.prism.repositories.TransactorFactory
import io.iohk.atala.prism.utils.GrpcUtils
import kamon.Kamon
import org.slf4j.LoggerFactory
import tofu.logging.Logs

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration

object ManagementConsoleApp extends IOApp {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val grpcConfig = GrpcUtils.GrpcConfig(port = 50054)

  implicit val ec = ExecutionContext.global

  override def run(args: List[String]): IO[ExitCode] = {
    Kamon.init()
    // Cats resource runs in multiple threads, we have to pass class loader
    // explicit to provide a proper resource directory.
    val classLoader = Thread.currentThread().getContextClassLoader
    app(classLoader).use { grpcServer =>
      logger.info("Starting GRPC server")
      grpcServer.start()
      sys.addShutdownHook {
        Await.result(Kamon.stop(), Duration.Inf)
      }
      IO.never
    }
  }

  def app(classLoader: ClassLoader): Resource[IO, Server] = {
    for {
      // configs
      globalConfig <- Resource.eval(IO.delay {
        logger.info("Loading config")
        ConfigFactory.load(classLoader)
      })
      defaultCredentialTypeConfig = DefaultCredentialTypeConfig(globalConfig)
      _ = logger.info("Setting-up uptime metrics")
      _ = Kamon.addReporter("uptime", new UptimeReporter(globalConfig))
      transactorConfig = TransactorFactory.transactorConfig(globalConfig)
      nodeConfig = NodeConfig(globalConfig)

      managementConsoleLogs = Logs.withContext[IO, IOWithTraceIdContext]

      // db
      tx <- TransactorFactory.transactor[IO](transactorConfig)
      txTraceIdLifted = tx.mapK(TraceId.liftToIOWithTraceId)
      _ <- TransactorFactory.runDbMigrations[IO](tx, classLoader)

      // node
      node = GrpcUtils.createPlaintextStub(
        host = nodeConfig.host,
        port = nodeConfig.port,
        stub = NodeServiceGrpc.stub
      )

      // connector
      connectorConfig = ConnectorClient.Config(
        globalConfig.getConfig("connector")
      )
      _ = logger.info(s"Connector config loaded: $connectorConfig")
      connector <- ConnectorClient.makeResource(
        connectorConfig,
        managementConsoleLogs
      )

      // repositories
      contactsRepository <- ContactsRepository.makeResource(
        txTraceIdLifted,
        managementConsoleLogs
      )
      participantsRepository <-
        ParticipantsRepository.makeResource(
          txTraceIdLifted,
          managementConsoleLogs,
          defaultCredentialTypeConfig
        )
      requestNoncesRepository <- RequestNoncesRepository.makeResource(
        txTraceIdLifted,
        managementConsoleLogs
      )
      statisticsRepository <- StatisticsRepository.makeResource(
        txTraceIdLifted,
        managementConsoleLogs
      )
      credentialsRepository <- CredentialsRepository.makeResource(
        txTraceIdLifted,
        managementConsoleLogs
      )
      receivedCredentialsRepository <-
        ReceivedCredentialsRepository.makeResource(
          txTraceIdLifted,
          managementConsoleLogs
        )
      institutionGroupsRepository <- InstitutionGroupsRepository.makeResource(
        txTraceIdLifted,
        managementConsoleLogs
      )
      credentialIssuancesRepository <-
        CredentialIssuancesRepository.makeResource(
          txTraceIdLifted,
          managementConsoleLogs
        )
      credentialTypeRepository <- CredentialTypeRepository.makeResource(
        txTraceIdLifted,
        managementConsoleLogs
      )

      contactsIntegrationService <-
        ContactsIntegrationService.makeResource(
          contactsRepository,
          connector,
          managementConsoleLogs
        )
      credentialIntegrationService <-
        CredentialsIntegrationService.makeResource(
          credentialsRepository,
          node,
          connector,
          managementConsoleLogs
        )
      participantsIntegrationService <-
        ParticipantsIntegrationService.makeResource(
          participantsRepository,
          managementConsoleLogs
        )
      credentialTypesService <- CredentialTypesService.makeResource(
        credentialTypeRepository,
        managementConsoleLogs
      )
      credentialsStoreService <-
        CredentialsStoreService.makeResource(
          receivedCredentialsRepository,
          managementConsoleLogs
        )
      credentialIssuanceService <-
        CredentialIssuanceService.makeResource(
          credentialIssuancesRepository,
          managementConsoleLogs
        )
      credentialsService <- CredentialsService.makeResource(
        credentialsRepository,
        credentialIntegrationService,
        node,
        connector,
        managementConsoleLogs
      )
      consoleService <-
        ConsoleService.makeResource(
          participantsIntegrationService,
          statisticsRepository,
          managementConsoleLogs
        )

      authenticator = new ManagementConsoleAuthenticator(
        participantsRepository,
        requestNoncesRepository,
        node,
        GrpcAuthenticationHeaderParser
      )

      groupsService <- GroupsService.makeResource(
        institutionGroupsRepository,
        managementConsoleLogs
      )

      credentialsStoreGrpcService =
        new CredentialsStoreGrpcService(
          credentialsStoreService,
          contactsRepository,
          authenticator
        )
      credentialsGrpcService = new CredentialsGrpcService(
        credentialsService,
        authenticator
      )
      groupsGrpcService = new GroupsGrpcService(groupsService, authenticator)
      consoleGrpcService = new ConsoleGrpcService(consoleService, authenticator)
      contactsGrpcService = new ContactsGrpcService(
        contactsIntegrationService,
        authenticator
      )
      credentialIssuanceGrpcService = new CredentialIssuanceGrpcService(
        credentialIssuanceService,
        authenticator
      )
      credentialTypesGrpcService = new CredentialTypesGrpcService(
        credentialTypesService,
        authenticator
      )

      // gRPC server
      grpcServer <- GrpcUtils.createGrpcServer[IO](
        grpcConfig,
        sslConfigOption = None,
        interceptor = Some(new GrpcAuthenticatorInterceptor),
        console_api.ConsoleServiceGrpc.bindService(consoleGrpcService, ec),
        console_api.ContactsServiceGrpc.bindService(contactsGrpcService, ec),
        console_api.CredentialIssuanceServiceGrpc
          .bindService(credentialIssuanceGrpcService, ec),
        console_api.CredentialsServiceGrpc
          .bindService(credentialsGrpcService, ec),
        console_api.GroupsServiceGrpc.bindService(groupsGrpcService, ec),
        console_api.CredentialsStoreServiceGrpc
          .bindService(credentialsStoreGrpcService, ec),
        console_api.CredentialTypesServiceGrpc
          .bindService(credentialTypesGrpcService, ec)
      )
    } yield grpcServer
  }
}
