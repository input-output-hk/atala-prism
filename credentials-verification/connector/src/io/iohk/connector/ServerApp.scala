package io.iohk.connector

import io.grpc.{Server, ServerBuilder}
import io.iohk.connector.protos._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Run with `mill -i connector.run`, otherwise, the server will stay running even after ctrl+C.
  */
object ServerApp {
  def main(args: Array[String]): Unit = {
    val server = new ServerApp(ExecutionContext.global)
    server.start()
    server.blockUntilShutdown()
  }

  private val port = 50051
}

class ServerApp(executionContext: ExecutionContext) { self =>
  private[this] var server: Server = null

  private def start(): Unit = {
    server = ServerBuilder
      .forPort(ServerApp.port)
      .addService(ConnectionsGrpc.bindService(new ConnectionsImpl, executionContext))
      .build()
      .start()

    println("Server started, listening on " + ServerApp.port)
    sys.addShutdownHook {
      System.err.println("*** shutting down gRPC server since JVM is shutting down")
      self.stop()
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

  private class ConnectionsImpl extends ConnectionsGrpc.Connections {
    override def acceptConnection(request: AcceptConnectionRequest): Future[AcceptConnectionReply] = {
      val reply = AcceptConnectionReply()
      Future.successful(reply)
    }

    override def newConnectionCode(request: NewConnectionCodeRequest): Future[NewConnectionCodeReply] = {
      val reply = NewConnectionCodeReply(code = "Hello ")
      Future.successful(reply)
    }
  }
}
