package io.iohk.atala.cvp.webextension.background

import scalapb.grpcweb.Metadata
import scalapb.grpc.Channels

import scala.concurrent.ExecutionContext.Implicits.global
import io.iohk.prism.protos.connector_api

//TODO Tobe deleted when actual implementation is started
object ClientTest {
  def run(): Unit = {
    println("Hello world!")

    val stub = connector_api.ConnectorServiceGrpcWeb.stub(Channels.grpcwebChannel("http://localhost:10000"))
    val req = connector_api.GetConnectionByTokenRequest(token = "noneYet")
    val header1 = "custom-header-1" -> "header-value"

    val metadata: Metadata = Metadata(header1)

    // Make an async unary call
    stub.getConnectionByToken(req, metadata).onComplete { f =>
      println("getConnectionByToken", f)
    }
  }
}
