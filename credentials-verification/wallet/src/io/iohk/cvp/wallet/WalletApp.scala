package io.iohk.cvp.wallet

import io.grpc.{Server, ServerBuilder}
import io.iohk.prism.protos.wallet_api
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext

object WalletApp {

  def main(args: Array[String]): Unit = {
    val app = new WalletApp()(ExecutionContext.global)
    app.start()
    app.blockUntilShutdown()
  }
}

class WalletApp()(implicit ec: ExecutionContext) {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val port = 50052

  private[this] var server: Server = null

  private def start(): Unit = {
    logger.info("Starting server")
    val walletService = new grpc.WalletServiceImpl
    server = ServerBuilder
      .forPort(port)
      .addService(wallet_api.WalletServiceGrpc.bindService(walletService, ec))
      .asInstanceOf[ServerBuilder[_]] // otherwise, IntelliJ marks the next lines as errors
      .build()
      .start()

    logger.info(s"Server started, listening on $port")
    sys.addShutdownHook {
      System.err.println("*** shutting down gRPC server since JVM is shutting down")
      stop()
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
}
