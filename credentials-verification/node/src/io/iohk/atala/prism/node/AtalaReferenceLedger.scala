package io.iohk.atala.prism.node

import java.time.Instant

import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.models.{
  BlockInfo,
  Ledger,
  TransactionDetails,
  TransactionId,
  TransactionInfo,
  TransactionStatus
}
import io.iohk.atala.prism.node.services.models.{AtalaObjectNotification, AtalaObjectNotificationHandler}
import io.iohk.atala.prism.protos.node_internal

import scala.concurrent.{ExecutionContext, Future}

trait AtalaReferenceLedger {
  def supportsOnChainData: Boolean

  def publish(obj: node_internal.AtalaObject): Future[PublicationInfo]

  def getTransactionDetails(transactionId: TransactionId): Future[TransactionDetails]

  def deleteTransaction(transactionId: TransactionId): Future[Unit]
}

case class PublicationInfo(transaction: TransactionInfo, status: TransactionStatus)

class InMemoryAtalaReferenceLedger(onAtalaObject: AtalaObjectNotificationHandler)(implicit ec: ExecutionContext)
    extends AtalaReferenceLedger {

  override def supportsOnChainData: Boolean = true

  override def publish(obj: node_internal.AtalaObject): Future[PublicationInfo] = {
    for {
      objectBytes <- Future.successful(obj.toByteArray)
      // Use a hash of the bytes as their in-memory transaction ID
      hash = SHA256Digest.compute(objectBytes)
      transactionId = TransactionId.from(hash.value).getOrElse(throw new RuntimeException("Unexpected invalid hash"))
      transactionInfo = TransactionInfo(
        transactionId = transactionId,
        ledger = Ledger.InMemory,
        // Used for informational purposes only, so fine to hard-code for testing
        block = Some(BlockInfo(number = 1, timestamp = Instant.now(), index = 1))
      )
      _ <- onAtalaObject(
        AtalaObjectNotification(obj, transactionInfo)
      )
    } yield PublicationInfo(transactionInfo, TransactionStatus.InLedger)
  }

  override def getTransactionDetails(transactionId: TransactionId): Future[TransactionDetails] = {
    // In-memory transactions are immediately in the ledger
    Future.successful(TransactionDetails(transactionId, TransactionStatus.InLedger))
  }

  override def deleteTransaction(transactionId: TransactionId): Future[Unit] = {
    Future.failed(new IllegalArgumentException("In-memory transactions cannot be deleted"))
  }
}
