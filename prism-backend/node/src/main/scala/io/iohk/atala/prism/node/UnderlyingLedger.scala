package io.iohk.atala.prism.node

import io.iohk.atala.prism.models.{Ledger, TransactionDetails, TransactionId, TransactionInfo, TransactionStatus}
import io.iohk.atala.prism.node.cardano.models.CardanoWalletError
import io.iohk.atala.prism.protos.node_internal

trait UnderlyingLedger[F[_]] {
  def getType: Ledger

  def publish(obj: node_internal.AtalaObject): F[Either[CardanoWalletError, PublicationInfo]]

  def getTransactionDetails(transactionId: TransactionId): F[Either[CardanoWalletError, TransactionDetails]]

  def deleteTransaction(transactionId: TransactionId): F[Either[CardanoWalletError, Unit]]
}

case class PublicationInfo(transaction: TransactionInfo, status: TransactionStatus)
