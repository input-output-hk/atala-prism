package io.iohk.atala.prism.node

import io.iohk.atala.prism.models.{Ledger, TransactionDetails, TransactionId, TransactionInfo, TransactionStatus}
import io.iohk.atala.prism.protos.node_internal

import scala.concurrent.Future

trait UnderlyingLedger {
  def getType: Ledger

  def supportsOnChainData: Boolean

  def publish(obj: node_internal.AtalaObject): Future[PublicationInfo]

  def getTransactionDetails(transactionId: TransactionId): Future[TransactionDetails]

  def deleteTransaction(transactionId: TransactionId): Future[Unit]
}

case class PublicationInfo(transaction: TransactionInfo, status: TransactionStatus)
