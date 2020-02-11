package io.iohk.cvp.admin

import com.typesafe.config.{Config, ConfigFactory}
import io.grpc.protobuf.services.ProtoReflectionService
import io.iohk.cvp.repositories.TransactorFactory
import org.slf4j.LoggerFactory
import io.grpc.ServerBuilder
import io.iohk.cvp.admin.AdminApp.AppContext

import scala.concurrent.ExecutionContext

object AdminApp {

  val logger = LoggerFactory.getLogger(AdminApp.getClass)

  def main(args: Array[String]): Unit = {
    new AdminApp(ExecutionContext.global).start()
  }

  class AppContext(config: Config)(implicit executionContext: ExecutionContext) {

    def transactorConfig(config: Config): TransactorFactory.Config = {
      val url = config.getString("url")
      val connectorPsqlUsername = config.getString("connector-psql-username")
      val connectorPsqlPassword = config.getString("connector-psql-password")
      TransactorFactory.Config(connectorPsqlUsername, connectorPsqlPassword, url)
    }

    val port = 50055

    val databaseConfig = transactorConfig(config.getConfig("db"))

    val xa = TransactorFactory(databaseConfig)

    val adminRepository = new AdminRepository(xa)

    val adminService = new AdminServiceImpl(adminRepository)

    val grpcService = AdminServiceGrpc.bindService(adminService, executionContext)

    val protoReflectionService = ProtoReflectionService.newInstance()

    val grpcServer = ServerBuilder
      .forPort(port)
      .addService(grpcService)
      .addService(protoReflectionService)
      .build()
  }
}

class AdminApp(executionContext: ExecutionContext) {

  import AdminApp.logger
  implicit val implicitExecutionContext: ExecutionContext = executionContext

  private def start(): Unit = {
    logger.info("Starting Admin service")
    val appContext = new AppContext(ConfigFactory.load())
    import appContext.grpcServer
    grpcServer.start()
    logger.info(s"Admin service listening on ${appContext.port}")

    sys.addShutdownHook {
      System.err.println("Shutting down Admin service (triggered by JVM shutdown)")
      grpcServer.shutdown()
      System.err.println("Admin service stopped")
    }
    grpcServer.awaitTermination()
  }
}
