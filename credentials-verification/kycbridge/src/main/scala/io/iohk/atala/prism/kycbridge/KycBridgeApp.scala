package io.iohk.atala.prism.kycbridge

import monix.eval.{Task, TaskApp}
import cats.effect.{ExitCode, Resource}
import com.typesafe.config.ConfigFactory
import doobie.hikari.HikariTransactor
import io.grpc.Server
import io.iohk.atala.prism.config.ConnectorConfig
import io.iohk.atala.prism.connector.RequestAuthenticator
import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.prism.kycbridge.config.KycBridgeConfig
import io.iohk.atala.prism.kycbridge.models.assureId.{Device, DeviceType}
import io.iohk.atala.prism.kycbridge.services.{
  AcasServiceImpl,
  AcuantService,
  AssureIdServiceImpl,
  ConnectionService,
  KycBridgeGrpcService,
  KycBridgeService
}
import io.iohk.atala.prism.repositories.TransactorFactory
import io.iohk.atala.prism.services.{ConnectorClientService, ConnectorClientServiceImpl}
import org.flywaydb.core.Flyway
import org.http4s.client.blaze.BlazeClientBuilder
import org.slf4j.LoggerFactory
import io.iohk.atala.kycbridge.protos.kycbridge_api.KycBridgeServiceGrpc
import io.iohk.atala.prism.utils.GrpcUtils

import scala.concurrent.ExecutionContext.Implicits.global

object KycBridgeApp extends TaskApp {

  private val logger = LoggerFactory.getLogger(this.getClass)

  override def run(args: List[String]): Task[ExitCode] = {
    val classLoader = Thread.currentThread().getContextClassLoader
    app(classLoader).use {
      case (assureIdService, acasService, grpcServer) =>
        logger.info("Kyc bridge application started")

        //only for demonstration purpose
        val device = Device(
          `type` = DeviceType(
            manufacturer = "manufacturer",
            model = "model"
          )
        )

        (for {
          documentResponse <- assureIdService.createNewDocumentInstance(device)
          _ = logger.info(s"New document response: $documentResponse")
          accessTokenResponse <- acasService.getAccessToken
          _ = logger.info(s"Access token response: $accessTokenResponse")
          documentStatus <- assureIdService.getDocumentStatus(
            documentResponse.toOption.get.documentId
          )
          _ = logger.info(s"Document status: $documentStatus")
          _ = logger.info("Starting GRPC server")
          _ = grpcServer.start
        } yield ()).flatMap(_ => Task.never)
    }
  }

  def app(classLoader: ClassLoader): Resource[Task, (AssureIdServiceImpl, AcasServiceImpl, Server)] =
    for {
      httpClient <- BlazeClientBuilder[Task](global).resource

      globalConfig = ConfigFactory.load(classLoader)

      kycBridgeConfig = KycBridgeConfig(globalConfig)
      connectorConfig = ConnectorConfig(globalConfig)
      transactorConfig = TransactorFactory.transactorConfig(globalConfig)

      tx <- TransactorFactory.transactorTask(transactorConfig)
      _ <- Resource.liftF(runMigrations(tx, classLoader))

      // connector
      connector = ConnectorClientService.createConnectorGrpcStub(connectorConfig)

      connectorService = new ConnectorClientServiceImpl(connector, new RequestAuthenticator(EC), connectorConfig)
      kycBridgeService = new KycBridgeService(tx, connectorService)
      kycBridgeGrpcService = new KycBridgeGrpcService(kycBridgeService)(scheduler)
      assureIdService = new AssureIdServiceImpl(kycBridgeConfig.acuantConfig, httpClient)
      acasService = new AcasServiceImpl(kycBridgeConfig.acuantConfig, httpClient)
      connectionService = new ConnectionService(tx, connectorService)
      acuantService = new AcuantService(tx, assureIdService, acasService, connectorService)

      _ <- Resource.liftF(connectionService.connectionUpdateStream.compile.drain.start)
      _ <- Resource.liftF(acuantService.acuantDataStream.compile.drain.start)

      // gRPC server
      grpcServer <- GrpcUtils.createGrpcServer(
        kycBridgeConfig.grpcConfig,
        KycBridgeServiceGrpc.bindService(kycBridgeGrpcService, scheduler)
      )

    } yield (assureIdService, acasService, grpcServer)

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
