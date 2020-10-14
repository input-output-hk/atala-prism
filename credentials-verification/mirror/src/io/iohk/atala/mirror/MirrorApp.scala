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
import io.iohk.atala.crypto.EC
import io.iohk.atala.requests.RequestAuthenticator
import io.iohk.atala.mirror.protos.mirror_api.MirrorServiceGrpc
import io.iohk.prism.protos.connector_api.ConnectorServiceGrpc
import io.iohk.atala.mirror.config.{ConnectorConfig, MirrorConfig, NodeConfig, TransactorConfig}
import io.iohk.atala.mirror.services.{
  ConnectorClientServiceImpl,
  CredentialService,
  MirrorService,
  NodeClientServiceImpl
}
import io.iohk.prism.protos.node_api.NodeServiceGrpc

object MirrorApp extends TaskApp {

  private val logger = LoggerFactory.getLogger(this.getClass)

  /**
    * Run the Mirror application.
    */
  override def run(args: List[String]): Task[ExitCode] =
    app.use { grpcServer =>
      logger.info("Starting GRPC server")
      grpcServer.start
      Task.never // run server forever
    }

  /**
    * This is an entry point for the Mirror application.
    * The resource contains a GRPC [[Server]] instance, that isn't started.
    */
  val app: Resource[Task, Server] =
    for {
      globalConfig <- Resource.liftF(Task {
        logger.info("Loading config")
        ConfigFactory.load()
      })

      // configs
      mirrorConfig = MirrorConfig(globalConfig)
      transactorConfig = TransactorConfig(globalConfig)
      connectorConfig = ConnectorConfig(globalConfig)
      nodeConfig = NodeConfig(globalConfig)

      // db
      tx <- createTransactor(transactorConfig)
      _ <- Resource.liftF(runMigrations(tx))

      // connector
      connector <- Resource.pure(createConnector(connectorConfig))

      // node
      node <- Resource.pure(createNode(nodeConfig))

      // services
      connectorService = new ConnectorClientServiceImpl(connector, new RequestAuthenticator(EC), connectorConfig)
      nodeService = new NodeClientServiceImpl(node)
      mirrorService = new MirrorService(tx, connectorService)
      credentialService = new CredentialService(tx, connectorService, nodeService)
      mirrorGrpcService = new MirrorGrpcService(mirrorService)(scheduler)

      // background streams
      _ <- Resource.liftF(credentialService.connectionUpdatesStream.compile.drain.start)
      _ <- Resource.liftF(credentialService.credentialUpdatesStream.compile.drain.start)

      // gRPC server
      grpcServer <- createGrpcServer(
        mirrorConfig,
        MirrorServiceGrpc.bindService(mirrorGrpcService, scheduler)
      )
    } yield grpcServer

  /**
    * Wrap a [[Server]] into a bracketed resource. The server stops when the
    * resource is released. With the following scenarios:
    *   - Server is shut down when there aren't any requests left.
    *   - We wait for 30 seconds to allow finish pending requests and
    *     then force quit the server.
    */
  def createGrpcServer(mirrorConfig: MirrorConfig, services: ServerServiceDefinition*): Resource[Task, Server] = {
    val builder = ServerBuilder.forPort(mirrorConfig.port)

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
        be.blockingContext // execute JDBC operations here
      )
    } yield xa

  /**
    * Run db migrations with Flyway.
    *
    * @return number of applied migrations
    */
  def runMigrations(transactor: HikariTransactor[Task]): Task[Int] =
    transactor.configure(dataSource =>
      Task(
        Flyway
          .configure()
          .dataSource(dataSource)
          .load()
          .migrate()
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
