package io.iohk.atala.mirror.services

import io.grpc.stub.StreamObserver
import io.iohk.atala.mirror.protos.trisa.{Transaction, TrisaPeer2PeerGrpc}
import org.slf4j.LoggerFactory

class TrisaPeer2PeerService extends TrisaPeer2PeerGrpc.TrisaPeer2Peer {

  private val logger = LoggerFactory.getLogger(classOf[TrisaPeer2PeerService])

  override def transactionStream(responseObserver: StreamObserver[Transaction]): StreamObserver[Transaction] =
    new StreamObserver[Transaction] {
      override def onNext(value: Transaction): Unit = {
        logger.info(s"Received transaction: $value")
      }

      override def onError(t: Throwable): Unit = {
        logger.info(s"Error occurred ${t.getMessage}")
      }

      override def onCompleted(): Unit = {
        logger.info("Trisa stream completed")
      }
    }
}
