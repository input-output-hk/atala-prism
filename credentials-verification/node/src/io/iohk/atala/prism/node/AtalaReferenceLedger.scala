package io.iohk.atala.prism.node

import java.time.Instant

import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.models.{Ledger, TransactionId, TransactionInfo}
import io.iohk.atala.prism.node.services.models.{
  AtalaObjectNotification,
  AtalaObjectNotificationHandler,
  AtalaObjectUpdate
}

import scala.concurrent.{ExecutionContext, Future}

trait AtalaReferenceLedger {
  def supportsOnChainData: Boolean
  def publishReference(ref: SHA256Digest): Future[TransactionInfo]
  def publishObject(bytes: Array[Byte]): Future[TransactionInfo]
}

class InMemoryAtalaReferenceLedger(onAtalaObject: AtalaObjectNotificationHandler)(implicit ec: ExecutionContext)
    extends AtalaReferenceLedger {

  override def supportsOnChainData: Boolean = true

  override def publishReference(ref: SHA256Digest): Future[TransactionInfo] = {
    // Use the ref itself as its in-memory transaction ID
    val transactionId = TransactionId.from(ref.value).getOrElse(throw new RuntimeException("Unexpected invalid hash"))
    val transactionInfo = TransactionInfo(transactionId, Ledger.InMemory)
    for {
      _ <- onAtalaObject(AtalaObjectNotification(AtalaObjectUpdate.Reference(ref), Instant.now(), transactionInfo))
    } yield transactionInfo
  }

  override def publishObject(bytes: Array[Byte]): Future[TransactionInfo] = {
    // Use a hash of the bytes as their in-memory transaction ID
    val hash = SHA256Digest.compute(bytes)
    val transactionId = TransactionId.from(hash.value).getOrElse(throw new RuntimeException("Unexpected invalid hash"))
    val transactionInfo = TransactionInfo(transactionId, Ledger.InMemory)
    for {
      _ <- onAtalaObject(AtalaObjectNotification(AtalaObjectUpdate.ByteContent(bytes), Instant.now(), transactionInfo))
    } yield transactionInfo
  }
}
