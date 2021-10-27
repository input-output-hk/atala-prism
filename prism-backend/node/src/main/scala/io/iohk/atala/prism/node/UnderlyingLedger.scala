package io.iohk.atala.prism.node

import io.iohk.atala.prism.models.{Ledger, TransactionDetails, TransactionId, TransactionInfo, TransactionStatus}
import io.iohk.atala.prism.node.cardano.models.CardanoWalletError
import io.iohk.atala.prism.protos.node_internal

import scala.concurrent.Future

trait UnderlyingLedger {
  def getType: Ledger

  def publish(
      obj: node_internal.AtalaObject
  ): Future[Either[CardanoWalletError, PublicationInfo]]

  def getTransactionDetails(
      transactionId: TransactionId
  ): Future[Either[CardanoWalletError, TransactionDetails]]

  def deleteTransaction(
      transactionId: TransactionId
  ): Future[Either[CardanoWalletError, Unit]]
}

case class PublicationInfo(
    transaction: TransactionInfo,
    status: TransactionStatus
)
