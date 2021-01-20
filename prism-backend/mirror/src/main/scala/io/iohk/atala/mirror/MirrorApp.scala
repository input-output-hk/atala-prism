package io.iohk.atala.mirror

import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import cats.effect.{ExitCode, Resource}
import monix.eval.{Task, TaskApp}
import io.grpc.Server
import org.flywaydb.core.Flyway
import doobie.hikari.HikariTransactor
import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.prism.connector.RequestAuthenticator
import io.iohk.atala.mirror.protos.mirror_api.MirrorServiceGrpc
import io.iohk.atala.mirror.config.MirrorConfig
import io.iohk.atala.mirror.http.ApiServer
import io.iohk.atala.mirror.http.endpoints.PaymentEndpoints
import io.iohk.atala.mirror.services.{
  CardanoAddressInfoService,
  CredentialService,
  MirrorService,
  TrisaPeer2PeerService,
  TrisaService
}
import io.iohk.atala.prism.config.{ConnectorConfig, NodeConfig}
import io.iohk.atala.prism.daos.ConnectorMessageOffsetDao
import io.iohk.atala.prism.models.CredentialProofRequestType
import io.iohk.atala.prism.repositories.TransactorFactory
import io.iohk.atala.prism.services.{
  ConnectorClientService,
  ConnectorClientServiceImpl,
  ConnectorMessagesService,
  NodeClientService,
  NodeClientServiceImpl
}
import io.iohk.atala.prism.utils.GrpcUtils
import doobie.implicits._
import io.iohk.atala.mirror.protos.trisa.TrisaPeer2PeerGrpc

object MirrorApp extends TaskApp {

  private val logger = LoggerFactory.getLogger(this.getClass)

  case class GrpcServers(mirrorGrpcServer: Server, trisaGrpcServer: Server)

  /**
    * Run the Mirror application.
    */
  override def run(args: List[String]): Task[ExitCode] = {
    //for some unknown reasons, incorrect class loader is used during execution
    //of Resource "for comprehension".
    val classLoader = Thread.currentThread().getContextClassLoader
    app(classLoader).use {
      case GrpcServers(grpcServer, trisaGrpcServer) =>
        logger.info("Starting GRPC server")
        grpcServer.start
        trisaGrpcServer.start()
        Task.never // run server forever
    }
  }

  /**
    * This is an entry point for the Mirror application.
    * The resource contains a GRPC [[Server]] instance, that isn't started.
    */
  def app(classLoader: ClassLoader): Resource[Task, GrpcServers] =
    for {
      globalConfig <- Resource.liftF(Task {
        logger.info("Loading config")
        ConfigFactory.load(classLoader)
      })

      // configs
      mirrorConfig = MirrorConfig(globalConfig)
      transactorConfig = TransactorFactory.transactorConfig(globalConfig)
      connectorConfig = ConnectorConfig(globalConfig)
      nodeConfig = NodeConfig(globalConfig)

      // db
      tx <- TransactorFactory.transactor[Task](transactorConfig)
      _ <- Resource.liftF(runMigrations(tx, classLoader))

      // connector
      connector <- Resource.liftF(Task.pure(ConnectorClientService.createConnectorGrpcStub(connectorConfig)))

      // node
      node <- Resource.liftF(Task.pure(NodeClientService.createNode(nodeConfig)))

      // services
      connectorService = new ConnectorClientServiceImpl(connector, new RequestAuthenticator(EC), connectorConfig)
      nodeService = new NodeClientServiceImpl(node, connectorConfig.authConfig)
      mirrorService = new MirrorService(tx, connectorService)
      credentialService = new CredentialService(tx, connectorService, nodeService)
      cardanoAddressInfoService = new CardanoAddressInfoService(tx, mirrorConfig.httpConfig, nodeService)
      mirrorGrpcService = new MirrorGrpcService(mirrorService)(scheduler)

      connectorMessageService = new ConnectorMessagesService(
        connectorService = connectorService,
        messageProcessors = List(
          credentialService.credentialMessageProcessor,
          cardanoAddressInfoService.cardanoAddressInfoMessageProcessor,
          cardanoAddressInfoService.payIdMessageProcessor,
          cardanoAddressInfoService.payIdNameRegistrationMessageProcessor
        ),
        findLastMessageOffset = ConnectorMessageOffsetDao.findLastMessageOffset().transact(tx),
        saveMessageOffset = messageId => ConnectorMessageOffsetDao.updateLastMessageOffset(messageId).transact(tx).void
      )
      trisaPeer2PeerService = new TrisaPeer2PeerService()

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

      //trisa
      _ = new TrisaService(mirrorConfig.trisaConfig).sendTestRequest("vasp1", 8091)

      // gRPC server
      grpcServer <- GrpcUtils.createGrpcServer(
        mirrorConfig.grpcConfig,
        sslConfigOption = None,
        MirrorServiceGrpc.bindService(mirrorGrpcService, scheduler)
      )

      // gRPC server
      trisaGrpcServer <- GrpcUtils.createGrpcServer(
        mirrorConfig.trisaConfig.grpcConfig,
        sslConfigOption = Some(mirrorConfig.trisaConfig.sslConfig),
        TrisaPeer2PeerGrpc.bindService(trisaPeer2PeerService, scheduler)
      )

      paymentsEndpoint = new PaymentEndpoints(cardanoAddressInfoService, mirrorConfig.httpConfig)
      apiServer = new ApiServer(paymentsEndpoint, mirrorConfig.httpConfig)
      _ <- apiServer.payIdServer.resource
    } yield GrpcServers(grpcServer, trisaGrpcServer)

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
}
