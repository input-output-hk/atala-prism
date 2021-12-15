package io.iohk.atala.prism.management.console

import cats.effect.unsafe.IORuntime
import cats.effect.{ExitCode, IO, IOApp, Resource}
import com.typesafe.config.{Config, ConfigFactory}
import io.grpc.Server
import io.iohk.atala.prism.auth.AuthenticatorF
import io.iohk.atala.prism.auth.grpc.{GrpcAuthenticatorInterceptor, TraceExposeInterceptor}
import io.iohk.atala.prism.config.NodeConfig
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.management.console.clients.ConnectorClient
import io.iohk.atala.prism.management.console.config.DefaultCredentialTypeConfig
import io.iohk.atala.prism.management.console.grpc.{ConsoleGrpcService, ContactsGrpcService, CredentialIssuanceGrpcService, CredentialTypeCategoryGrpcService, CredentialTypesGrpcService, CredentialsGrpcService, CredentialsStoreGrpcService, GroupsGrpcService}
import io.iohk.atala.prism.management.console.integrations.{ContactsIntegrationService, CredentialsIntegrationService, ParticipantsIntegrationService}
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

import scala.concurrent.ExecutionContext

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
    // It is important for this implicit to not be in the class scope as the underlying `runtime` might not have
    // been initialized. By putting it here we make sure that it has been already been initialized. Refer to `IOApp`'s
    // implementation for more insight.
    implicit val ioRuntime: IORuntime = runtime

    for {
      // configs
      globalConfig <- Resource.eval(IO.delay {
        logger.info("Loading config")
        ConfigFactory.load(classLoader)
      })
      _ <- startMetrics(globalConfig)
      defaultCredentialTypeConfig = DefaultCredentialTypeConfig(globalConfig)
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
      credentialTypeCategoryRepository <- CredentialTypeCategoryRepository.makeResource(
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
      credentialTypeCategoryService <- CredentialTypeCategoryService.makeResource(
        credentialTypeCategoryRepository,
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

      authenticator <- AuthenticatorF.resource(
        node,
        new ManagementConsoleAuthenticator(
          participantsRepository,
          requestNoncesRepository
        ),
        managementConsoleLogs
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
      credentialTypeCategoryGrpcService = new CredentialTypeCategoryGrpcService(
        credentialTypeCategoryService,
        authenticator
      )

      // gRPC server
      grpcServer <- GrpcUtils.createGrpcServer[IO](
        grpcConfig,
        sslConfigOption = None,
        interceptor = List(new TraceExposeInterceptor, new GrpcAuthenticatorInterceptor),
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
          .bindService(credentialTypesGrpcService, ec),
        console_api.CredentialTypeCategoriesServiceGrpc.bindService(credentialTypeCategoryGrpcService, ec)
      )
    } yield grpcServer
  }

  private def startMetrics(config: Config): Resource[IO, Unit] = Resource.make(IO {
    logger.info("Setting-up uptime metrics")
    Kamon.init()
    Kamon.addReporter("uptime", new UptimeReporter(config))
    ()
  })(_ => IO.fromFuture(IO(Kamon.stop())))
}
