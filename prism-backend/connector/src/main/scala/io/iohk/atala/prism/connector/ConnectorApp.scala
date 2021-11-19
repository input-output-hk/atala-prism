package io.iohk.atala.prism.connector

import cats.effect.unsafe.IORuntime
import cats.effect.{ExitCode, IO, IOApp, Resource}
import com.typesafe.config.{Config, ConfigFactory}
import doobie.hikari.HikariTransactor
import io.grpc.{ManagedChannelBuilder, Server, ServerBuilder}
import io.iohk.atala.prism.auth.AuthenticatorF
import io.iohk.atala.prism.auth.grpc._
import io.iohk.atala.prism.connector.repositories._
import io.iohk.atala.prism.connector.services._
import io.iohk.atala.prism.cviews.CredentialViewsService
import io.iohk.atala.prism.identity.PrismDid
import io.iohk.atala.prism.intdemo.ConnectorIntegration.ConnectorIntegrationImpl
import io.iohk.atala.prism.intdemo._
import io.iohk.atala.prism.intdemo.protos.intdemo_api.{
  DegreeServiceGrpc,
  EmploymentServiceGrpc,
  IDServiceGrpc,
  InsuranceServiceGrpc
}
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.metrics.UptimeReporter
import io.iohk.atala.prism.protos.connector_api
import io.iohk.atala.prism.protos.connector_api.ContactConnectionServiceGrpc
import io.iohk.atala.prism.protos.cviews_api.CredentialViewsServiceGrpc
import io.iohk.atala.prism.protos.node_api.NodeServiceGrpc
import io.iohk.atala.prism.repositories.{SchemaMigrations, TransactorFactory}
import io.iohk.atala.prism.utils.IOUtils._
import kamon.Kamon
import kamon.module.Module
import org.slf4j.LoggerFactory
import tofu.logging.Logs

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

object ConnectorApp extends IOApp {
  private val port = 50051

  override def run(args: List[String]): IO[ExitCode] = {
    val server = new ConnectorApp()(ExecutionContext.global)
    server.start().use(_ => IO.never)
  }
}

class ConnectorApp(implicit executionContext: ExecutionContext) { self =>
  private val logger = LoggerFactory.getLogger(this.getClass)

  implicit val ec: ExecutionContextExecutor = ExecutionContext.global
  implicit val runtime: IORuntime = IORuntime.global

  private def start(): Resource[IO, Server] = {
    for {
      globalConfig <- loadConfig
      _ <- startMetrics(globalConfig)
      databaseConfig = TransactorFactory.transactorConfig(globalConfig)
      _ = applyMigrations(databaseConfig)
      tx <- connectToTheDb(databaseConfig)
      whitelistDid = loadWhitelistDid(globalConfig)
      txTraceIdLifted = tx.mapK(TraceId.liftToIOWithTraceId)
      connectorLogs = Logs.withContext[IO, IOWithTraceIdContext]
      node = initNodeClient(globalConfig)
      // connector repositories
      connectionsRepository <- ConnectionsRepository.resource(
        txTraceIdLifted,
        connectorLogs
      )
      messagesRepository <- MessagesRepository.resource(
        txTraceIdLifted,
        connectorLogs
      )
      requestNoncesRepository <- RequestNoncesRepository.resource(
        txTraceIdLifted,
        connectorLogs
      )
      participantsRepository <- ParticipantsRepository.resource(
        txTraceIdLifted,
        connectorLogs
      )
      // authenticator
      authenticator = new ConnectorAuthenticator(
        participantsRepository,
        requestNoncesRepository,
        node,
        GrpcAuthenticationHeaderParser
      )

      // authenticatorF
      authenticatorF <- AuthenticatorF.resource(
        node,
        new ConnectorAuthenticatorF(
          participantsRepository,
          requestNoncesRepository
        ),
        connectorLogs
      )
      // Background services
      messageNotificationService <- MessageNotificationService.resourceAndStart(tx)
      // connector services
      connectionsService <- ConnectionsService.resource(
        connectionsRepository,
        node,
        connectorLogs
      )
      messagesService <- MessagesService.resource[IOWithTraceIdContext, IO](
        messagesRepository,
        connectorLogs
      )
      registrationService <- RegistrationService
        .resource[IOWithTraceIdContext, IO](
          participantsRepository,
          node,
          connectorLogs
        )
      contactConnectionService = new ContactConnectionService(
        connectionsService,
        authenticator,
        whitelistDid
      )
      connectorService = new ConnectorService(
        connectionsService,
        messagesService,
        registrationService,
        messageNotificationService,
        authenticatorF,
        node,
        participantsRepository
      )
      credentialViewsService = new CredentialViewsService(authenticator)(
        executionContext
      )
      // interactive demo services
      intDemoRepository = new IntDemoRepository(tx)
      connectorIntegration = new ConnectorIntegrationImpl(
        connectionsService,
        messagesService
      )
      idService = new IdServiceImpl(
        connectorIntegration,
        intDemoRepository,
        schedulerPeriod = 1.second
      )
      degreeService = new DegreeServiceImpl(
        connectorIntegration,
        intDemoRepository,
        schedulerPeriod = 1.second
      )
      employmentService = new EmploymentServiceImpl(
        connectorIntegration,
        intDemoRepository,
        schedulerPeriod = 1.second
      )
      insuranceService = new InsuranceServiceImpl(
        connectorIntegration,
        intDemoRepository,
        schedulerPeriod = 1.second
      )
      server <- startServer(
        connectorService,
        credentialViewsService,
        idService,
        degreeService,
        employmentService,
        insuranceService,
        contactConnectionService,
        executionContext
      )
    } yield server
  }

  private def startMetrics(config: Config): Resource[IO, Module.Registration] =
    Resource.make(IO {
      logger.info("Setting-up uptime metrics")
      Kamon.init()
      Kamon.addReporter("uptime", new UptimeReporter(config))
    }) { _ => IO.fromFuture(IO(Kamon.stop())) }

  private def loadConfig: Resource[IO, Config] =
    Resource.pure[IO, Config] {
      logger.info("Loading config")
      ConfigFactory.load()
    }

  private def applyMigrations(
      databaseConfig: TransactorFactory.Config
  ): Unit = {
    logger.info("Applying database migrations")
    applyDatabaseMigrations(databaseConfig)
  }

  private def connectToTheDb(
      databaseConfig: TransactorFactory.Config
  ): Resource[IO, HikariTransactor[IO]] = {
    logger.info("Connecting to the database")
    TransactorFactory.transactor[IO](databaseConfig)
  }

  private def loadWhitelistDid(config: Config): Set[PrismDid] = {
    logger.info("Loading DID whitelist")
    val didWhitelist = DidWhitelistLoader.load(config)
    if (didWhitelist.isEmpty) {
      logger.warn(
        s"DID whitelist is empty, which prevents integrating the console backend"
      )
    } else {
      logger.info(
        s"DID whitelist:\n${didWhitelist.map(_.getValue).map("- " + _).mkString("\n")}"
      )
    }
    didWhitelist
  }

  private def initNodeClient(
      config: Config
  ): NodeServiceGrpc.NodeServiceStub = {
    val configLoader = new ConfigLoader
    val nodeConfig =
      configLoader.nodeClientConfig(config.getConfig("node"))
    val nodeChannel = ManagedChannelBuilder
      .forAddress(nodeConfig.host, nodeConfig.port)
      .intercept(new ClientTraceReadInterceptor)
      .usePlaintext()
      .build()
    NodeServiceGrpc.stub(nodeChannel)
  }

  private def startServer(
      connectorService: ConnectorService,
      credentialViewsService: CredentialViewsService,
      idService: IdServiceImpl,
      degreeService: DegreeServiceImpl,
      employmentService: EmploymentServiceImpl,
      insuranceService: InsuranceServiceImpl,
      contactConnectionService: ContactConnectionService,
      executionContext: ExecutionContext
  ): Resource[IO, Server] = Resource.make(IO {
    logger.info("Starting server")
    val server = ServerBuilder
      .forPort(ConnectorApp.port)
      .intercept(new TraceExposeInterceptor)
      .intercept(new TraceReadInterceptor)
      .intercept(new GrpcAuthenticatorInterceptor)
      .addService(
        _root_.grpc.health.v1.health.HealthGrpc
          .bindService(new HealthService, executionContext)
      )
      .addService(
        connector_api.ConnectorServiceGrpc.bindService(
          connectorService,
          executionContext
        )
      )
      .addService(
        CredentialViewsServiceGrpc.bindService(
          credentialViewsService,
          executionContext
        )
      )
      .addService(IDServiceGrpc.bindService(idService, executionContext))
      .addService(
        DegreeServiceGrpc.bindService(degreeService, executionContext)
      )
      .addService(
        EmploymentServiceGrpc.bindService(employmentService, executionContext)
      )
      .addService(
        InsuranceServiceGrpc.bindService(insuranceService, executionContext)
      )
      .addService(
        ContactConnectionServiceGrpc
          .bindService(contactConnectionService, executionContext)
      )
      .build()
      .start()
    logger.info("Server started, listening on " + ConnectorApp.port)
    server
  })(server =>
    IO {
      System.err.println(
        "*** shutting down gRPC server since JVM is shutting down"
      )
      server.shutdown()
      server.awaitTermination()
      System.err.println("*** server shut down")
    }
  )

  def applyDatabaseMigrations(
      databaseConfig: TransactorFactory.Config
  ): Unit = {
    val appliedMigrations = SchemaMigrations.migrate(databaseConfig)
    if (appliedMigrations == 0) {
      logger.info("Database up to date")
    } else {
      logger.info(s"$appliedMigrations migration scripts applied")
    }
  }
}
