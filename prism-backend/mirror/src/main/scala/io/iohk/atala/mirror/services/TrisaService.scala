package io.iohk.atala.mirror.services

import io.grpc.netty.{GrpcSslContexts, NettyChannelBuilder}
import io.iohk.atala.mirror.protos.trisa.{Transaction, TrisaPeer2PeerGrpc}
import io.grpc.stub.StreamObserver
import io.iohk.atala.mirror.config.TrisaConfig
import org.slf4j.LoggerFactory

import java.io.File
import scala.util.Try

class TrisaService(trisaConfig: TrisaConfig) {

  private val logger = LoggerFactory.getLogger(classOf[TrisaService])

  private val sslContext = {
    val sslContext = GrpcSslContexts.forClient()

    sslContext.trustManager(new File(trisaConfig.serverTrustChainLocation))
    sslContext.keyManager(
      new File(trisaConfig.serverCertificateLocation),
      new File(trisaConfig.serverCertificatePrivateKeyLocation)
    )

    sslContext.build()
  }

  def connect(host: String, port: Int): TrisaPeer2PeerGrpc.TrisaPeer2PeerStub = {
    val channel = NettyChannelBuilder
      .forAddress(host, port)
      .sslContext(sslContext)
      .build()

    TrisaPeer2PeerGrpc.stub(channel)
  }

  def sendTestRequest(host: String, port: Int): Unit = {
    Try {
      val stub = connect(host, port)

      val responseObserver = new StreamObserver[Transaction] {
        override def onNext(value: Transaction): Unit = {
          logger.info(s"Received transaction: ${value}")
        }

        override def onError(t: Throwable): Unit = {
          logger.info(s"Error occurred ${t.getMessage}")
        }

        override def onCompleted(): Unit = {
          logger.info("Trisa stream completed")
        }
      }

      val stream = stub.transactionStream(responseObserver)

      stream.onNext(Transaction())
    }.recover { e => logger.warn(e.getMessage) }
    ()
  }

}
