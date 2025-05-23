package io.iohk.atala.prism.node

import derevo.derive
import derevo.tagless.applyK
import io.iohk.atala.prism.node.models._
import io.iohk.atala.prism.node.cardano.models.CardanoWalletError
import io.iohk.atala.prism.node.models.Balance
import io.iohk.atala.prism.protos.node_models

@derive(applyK)
trait UnderlyingLedger[F[_]] {
  def getType: Ledger

  def publish(
      obj: node_models.AtalaObject
  ): F[Either[CardanoWalletError, PublicationInfo]]

  def getTransactionDetails(
      transactionId: TransactionId
  ): F[Either[CardanoWalletError, TransactionDetails]]

  def deleteTransaction(
      transactionId: TransactionId
  ): F[Either[CardanoWalletError, Unit]]

  def getWalletBalance: F[Either[CardanoWalletError, Balance]]
}

case class PublicationInfo(
    transaction: TransactionInfo,
    status: TransactionStatus
)
