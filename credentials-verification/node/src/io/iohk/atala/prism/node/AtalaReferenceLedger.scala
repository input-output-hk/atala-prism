package io.iohk.atala.prism.node

import java.time.Instant

import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.models.{Ledger, TransactionId, TransactionInfo}
import io.iohk.atala.prism.node.services.models.{AtalaObjectNotification, AtalaObjectNotificationHandler}
import io.iohk.prism.protos.node_internal

import scala.concurrent.{ExecutionContext, Future}

trait AtalaReferenceLedger {
  def supportsOnChainData: Boolean
  def publish(obj: node_internal.AtalaObject): Future[TransactionInfo]
}

class InMemoryAtalaReferenceLedger(onAtalaObject: AtalaObjectNotificationHandler)(implicit ec: ExecutionContext)
    extends AtalaReferenceLedger {

  override def supportsOnChainData: Boolean = true

  override def publish(obj: node_internal.AtalaObject): Future[TransactionInfo] = {
    for {
      objectBytes <- Future.successful(obj.toByteArray)
      // Use a hash of the bytes as their in-memory transaction ID
      hash = SHA256Digest.compute(objectBytes)
      transactionId = TransactionId.from(hash.value).getOrElse(throw new RuntimeException("Unexpected invalid hash"))
      transactionInfo = TransactionInfo(transactionId, Ledger.InMemory)
      _ <- onAtalaObject(
        AtalaObjectNotification(obj, Instant.now(), transactionInfo)
      )
    } yield transactionInfo
  }
}
