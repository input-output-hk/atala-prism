package io.iohk.atala.prism.node.cardano.wallet.metrics

import cats.effect.BracketThrow
import io.iohk.atala.prism.metrics.TimeMeasureUtil.{DomainTimer, MeasureOps}
import io.iohk.atala.prism.metrics.{TimeMeasureMetric, TimeMeasureUtil}
import io.iohk.atala.prism.models.{TransactionDetails, TransactionId}
import io.iohk.atala.prism.node.cardano.models.{Payment, TransactionMetadata, WalletId}
import io.iohk.atala.prism.node.cardano.wallet.CardanoWalletApiClient
import io.iohk.atala.prism.node.cardano.wallet.CardanoWalletApiClient.Result
import io.iohk.atala.prism.node.models.WalletDetails
import tofu.higherKind.Mid

private[wallet] final class CardanoWalletApiClientMetrics[F[
    _
]: TimeMeasureMetric: BracketThrow]
    extends CardanoWalletApiClient[Mid[F, *]] {

  val clientName = "CardanoWalletApiClient"
  val estimateTransactionFeeTimer: DomainTimer =
    TimeMeasureUtil.createClientRequestTimer(
      clientName,
      "estimateTransactionFee"
    )
  val postTransactionTimer: DomainTimer =
    TimeMeasureUtil.createClientRequestTimer(clientName, "postTransaction")
  val getTransactionTimer: DomainTimer =
    TimeMeasureUtil.createClientRequestTimer(clientName, "getTransaction")
  val deleteTransactionTimer: DomainTimer =
    TimeMeasureUtil.createClientRequestTimer(clientName, "deleteTransaction")
  val getWalletTimer: DomainTimer =
    TimeMeasureUtil.createClientRequestTimer(clientName, "getWallet")

  override def estimateTransactionFee(
      walletId: WalletId,
      payments: List[Payment],
      metadata: Option[TransactionMetadata]
  ): Mid[F, Result[CardanoWalletApiClient.EstimatedFee]] =
    _.measureOperationTime(estimateTransactionFeeTimer)

  override def postTransaction(
      walletId: WalletId,
      payments: List[Payment],
      metadata: Option[TransactionMetadata],
      passphrase: String
  ): Mid[F, Result[TransactionId]] =
    _.measureOperationTime(postTransactionTimer)

  override def getTransaction(
      walletId: WalletId,
      transactionId: TransactionId
  ): Mid[F, Result[TransactionDetails]] =
    _.measureOperationTime(getTransactionTimer)

  override def deleteTransaction(
      walletId: WalletId,
      transactionId: TransactionId
  ): Mid[F, Result[Unit]] =
    _.measureOperationTime(deleteTransactionTimer)

  override def getWallet(walletId: WalletId): Mid[F, Result[WalletDetails]] =
    _.measureOperationTime(getWalletTimer)
}
