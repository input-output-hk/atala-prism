package io.iohk.atala.prism.node

import java.time.Instant

import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.node.models.TransactionId
import io.iohk.atala.prism.node.services.models.{AtalaObjectUpdate, ObjectHandler}

import scala.concurrent.{ExecutionContext, Future}

trait AtalaReferenceLedger {
  def supportsOnChainData: Boolean
  def publishReference(ref: SHA256Digest): Future[TransactionId]
  def publishObject(bytes: Array[Byte]): Future[TransactionId]
}

class InMemoryAtalaReferenceLedger(onNewObject: ObjectHandler)(implicit ec: ExecutionContext)
    extends AtalaReferenceLedger {

  override def supportsOnChainData: Boolean = true

  override def publishReference(ref: SHA256Digest): Future[TransactionId] = {
    // Use the ref itself as its in-memory transaction ID
    val transactionId = TransactionId.from(ref.value).getOrElse(throw new RuntimeException("Unexpected invalid hash"))
    for {
      _ <- onNewObject(AtalaObjectUpdate.Reference(ref), Instant.now())
    } yield transactionId
  }

  override def publishObject(bytes: Array[Byte]): Future[TransactionId] = {
    // Use a hash of the bytes as their in-memory transaction ID
    val hash = SHA256Digest.compute(bytes)
    val transactionId = TransactionId.from(hash.value).getOrElse(throw new RuntimeException("Unexpected invalid hash"))
    for {
      _ <- onNewObject(AtalaObjectUpdate.ByteContent(bytes), Instant.now())
    } yield transactionId
  }
}
