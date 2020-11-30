package io.iohk.atala.mirror

import java.util.concurrent.TimeUnit

import scala.concurrent.blocking
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import cats.effect.{Blocker, ExitCode, Resource}
import monix.eval.{Task, TaskApp}
import io.grpc.{ManagedChannelBuilder, Server, ServerBuilder, ServerServiceDefinition}
import io.grpc.protobuf.services.ProtoReflectionService
import org.flywaydb.core.Flyway
import doobie.util.ExecutionContexts
import doobie.hikari.HikariTransactor
import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.prism.connector.RequestAuthenticator
import io.iohk.atala.mirror.protos.mirror_api.MirrorServiceGrpc
import io.iohk.atala.prism.protos.connector_api.ConnectorServiceGrpc
import io.iohk.atala.mirror.config.{ConnectorConfig, MirrorConfig, NodeConfig, TransactorConfig}
import io.iohk.atala.mirror.http.ApiServer
import io.iohk.atala.mirror.http.endpoints.PaymentEndpoints
import io.iohk.atala.mirror.services.{
  CardanoAddressInfoService,
  ConnectorClientServiceImpl,
  ConnectorMessagesService,
  CredentialService,
  MirrorService,
  NodeClientServiceImpl
}
import io.iohk.atala.prism.protos.node_api.NodeServiceGrpc
import io.iohk.atala.mirror.models.CredentialProofRequestType

object MirrorApp extends TaskApp {

  private val logger = LoggerFactory.getLogger(this.getClass)

  /**
    * Run the Mirror application.
    */
  override def run(args: List[String]): Task[ExitCode] = {
    //for some unknown reasons, incorrect class loader is used during execution
    //of Resource "for comprehension".
    val classLoader = Thread.currentThread().getContextClassLoader
    app(classLoader).use { grpcServer =>
      logger.info("Starting GRPC server")
      grpcServer.start
      Task.never // run server forever
    }
  }

  /**
    * This is an entry point for the Mirror application.
    * The resource contains a GRPC [[Server]] instance, that isn't started.
    */
  def app(classLoader: ClassLoader): Resource[Task, Server] =
    for {
      globalConfig <- Resource.liftF(Task {
        logger.info("Loading config")
        ConfigFactory.load(classLoader)
      })

      // configs
      mirrorConfig = MirrorConfig(globalConfig)
      transactorConfig = TransactorConfig(globalConfig)
      connectorConfig = ConnectorConfig(globalConfig)
      nodeConfig = NodeConfig(globalConfig)

      // db
      tx <- createTransactor(transactorConfig)
      _ <- Resource.liftF(runMigrations(tx, classLoader))

      // connector
      connector <- Resource.pure(createConnector(connectorConfig))

      // node
      node <- Resource.pure(createNode(nodeConfig))

      // services
      connectorService = new ConnectorClientServiceImpl(connector, new RequestAuthenticator(EC), connectorConfig)
      nodeService = new NodeClientServiceImpl(node, connectorConfig.authConfig)
      mirrorService = new MirrorService(tx, connectorService)
      credentialService = new CredentialService(tx, connectorService, nodeService)
      cardanoAddressInfoService = new CardanoAddressInfoService(tx, mirrorConfig.httpConfig, nodeService)
      mirrorGrpcService = new MirrorGrpcService(mirrorService)(scheduler)

      connectorMessageService = new ConnectorMessagesService(
        tx,
        connectorService,
        List(
          credentialService.credentialMessageProcessor,
          cardanoAddressInfoService.cardanoAddressInfoMessageProcessor,
          cardanoAddressInfoService.payIdMessageProcessor,
          cardanoAddressInfoService.payIdNameRegistrationMessageProcessor
        )
      )

      // background streams
      _ <- Resource.liftF(
        credentialService
          .connectionUpdatesStream(
            // TODO: We are sending unsigned credential form intdemo by default, it allows
            //       to test the Mirror with the mobile apps, to check signed flow, see: [[MirrorE2eSpec]].
            immediatelyRequestedCredential = CredentialProofRequestType.RedlandIdCredential
          )
          .compile
          .drain
          .start
      )
      _ <- Resource.liftF(connectorMessageService.messagesUpdatesStream.compile.drain.start)

      // gRPC server
      grpcServer <- createGrpcServer(
        mirrorConfig,
        MirrorServiceGrpc.bindService(mirrorGrpcService, scheduler)
      )

      paymentsEndpoint = new PaymentEndpoints(cardanoAddressInfoService, mirrorConfig.httpConfig)
      apiServer = new ApiServer(paymentsEndpoint, mirrorConfig.httpConfig)
      _ <- apiServer.payIdServer.resource
    } yield grpcServer

  /**
    * Wrap a [[Server]] into a bracketed resource. The server stops when the
    * resource is released. With the following scenarios:
    *   - Server is shut down when there aren't any requests left.
    *   - We wait for 30 seconds to allow finish pending requests and
    *     then force quit the server.
    */
  def createGrpcServer(mirrorConfig: MirrorConfig, services: ServerServiceDefinition*): Resource[Task, Server] = {
    val builder = ServerBuilder.forPort(mirrorConfig.grpcConfig.port)

    builder.addService(
      ProtoReflectionService.newInstance()
    ) // TODO: Decide before release if we should keep this (or guard it with a config flag)
    services.foreach(builder.addService(_))

    val shutdown = (server: Server) =>
      Task {
        logger.info("Stopping GRPC server")
        server.shutdown()
        if (!blocking(server.awaitTermination(30, TimeUnit.SECONDS))) {
          server.shutdownNow()
        }
      }.void

    Resource.make(Task(builder.build()))(shutdown)
  }

  /**
    * Create resource with a db transactor.
    */
  def createTransactor(transactorConfig: TransactorConfig): Resource[Task, HikariTransactor[Task]] =
    for {
      ce <- ExecutionContexts.fixedThreadPool[Task](32) // connection EC
      be <- Blocker[Task] // blocking EC
      xa <- HikariTransactor.newHikariTransactor[Task](
        transactorConfig.driver,
        transactorConfig.jdbcUrl,
        transactorConfig.username,
        transactorConfig.password,
        ce, // await connection here
        be // execute JDBC operations here
      )
    } yield xa

  /**
    * Run db migrations with Flyway.
    *
    * @return number of applied migrations
    */
  def runMigrations(transactor: HikariTransactor[Task], classLoader: ClassLoader): Task[Int] =
    transactor.configure(dataSource =>
      Task(
        Flyway
          .configure(classLoader)
          .dataSource(dataSource)
          .load()
          .migrate()
          .migrationsExecuted
      )
    )

  /**
    * Create a connector gRPC service stub.
    */
  def createConnector(
      connectorConfig: ConnectorConfig
  ): ConnectorServiceGrpc.ConnectorServiceStub = {
    val channel = ManagedChannelBuilder
      .forAddress(connectorConfig.host, connectorConfig.port)
      .usePlaintext()
      .build()

    ConnectorServiceGrpc.stub(channel)
  }

  /**
    * Create a node gRPC service stub.
    */
  def createNode(
      nodeConfig: NodeConfig
  ): NodeServiceGrpc.NodeServiceStub = {
    val channel = ManagedChannelBuilder
      .forAddress(nodeConfig.host, nodeConfig.port)
      .usePlaintext()
      .build()

    NodeServiceGrpc.stub(channel)
  }

}
