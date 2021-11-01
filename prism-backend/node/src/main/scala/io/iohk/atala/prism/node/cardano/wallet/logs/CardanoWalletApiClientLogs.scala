package io.iohk.atala.prism.node.cardano.wallet.logs

import cats.syntax.apply._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import io.iohk.atala.prism.models.{TransactionDetails, TransactionId}
import io.iohk.atala.prism.node.cardano.models.{Payment, TransactionMetadata, WalletId}
import io.iohk.atala.prism.node.cardano.wallet.CardanoWalletApiClient
import io.iohk.atala.prism.node.cardano.wallet.CardanoWalletApiClient.Result
import io.iohk.atala.prism.node.models.WalletDetails
import tofu.higherKind.Mid
import tofu.logging.ServiceLogging
import tofu.syntax.logging._
import cats.MonadThrow

private[wallet] final class CardanoWalletApiClientLogs[F[_]: ServiceLogging[
  *[_],
  CardanoWalletApiClient[F]
]: MonadThrow]
    extends CardanoWalletApiClient[Mid[F, *]] {

  /** Estimate the fee for the given transaction details.
    *
    * @see
    *   https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/postTransactionFee
    */
  override def estimateTransactionFee(
      walletId: WalletId,
      payments: List[Payment],
      metadata: Option[TransactionMetadata]
  ): Mid[F, Result[CardanoWalletApiClient.EstimatedFee]] =
    in =>
      info"estimating transaction fee" *> in
        .flatTap(
          _.fold(
            err => error"Encountered an error while estimating transaction fee $err",
            res => info"estimating transaction fee - successfully done, $res"
          )
        )
        .onError(
          errorCause"Encountered an error while estimating transaction fee" (_)
        )

  /** Post a new transaction and return its ID.
    *
    * @see
    *   https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/postTransaction
    */
  override def postTransaction(
      walletId: WalletId,
      payments: List[Payment],
      metadata: Option[TransactionMetadata],
      passphrase: String
  ): Mid[F, Result[TransactionId]] =
    in =>
      info"posting transaction" *> in
        .flatTap(
          _.fold(
            err => error"Encountered an error while posting transaction $err",
            tid => info"posting transaction - successfully done $tid"
          )
        )
        .onError(
          errorCause"Encountered an error while estimating transaction fee" (_)
        )

  /** Get the details of the given transaction.
    *
    * @see
    *   https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/getTransaction
    */
  override def getTransaction(
      walletId: WalletId,
      transactionId: TransactionId
  ): Mid[F, Result[TransactionDetails]] =
    in =>
      info"getting transaction" *> in
        .flatTap(
          _.fold(
            err => error"Encountered an error while getting transaction $err",
            transaction => info"getting transaction - successfully done ${transaction.id}"
          )
        )
        .onError(errorCause"Encountered an error while getting transaction" (_))

  /** Forget pending transaction. Importantly, a transaction, when sent, cannot be cancelled. One can only request
    * forgetting about it in order to try spending (concurrently) the same UTxO in another transaction.
    *
    * <p>Note the transaction may still show up later in a block.
    *
    * @see
    *   https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/deleteTransaction
    */
  override def deleteTransaction(
      walletId: WalletId,
      transactionId: TransactionId
  ): Mid[F, Result[Unit]] =
    in =>
      info"deleting transaction" *> in
        .flatTap(
          _.fold(
            err => error"Encountered an error while deleting transaction $err",
            _ => info"deleting transaction - successfully done"
          )
        )
        .onError(
          errorCause"Encountered an error while deleting transaction" (_)
        )

  /** Get detailed information about the given wallet.
    *
    * @see
    *   https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/getWallet
    */
  override def getWallet(walletId: WalletId): Mid[F, Result[WalletDetails]] =
    in =>
      info"getting wallet" *> in
        .flatTap(
          _.fold(
            err => error"Encountered an error while getting wallet $err",
            _ => info"getting wallet - successfully done"
          )
        )
        .onError(errorCause"Encountered an error while getting wallet" (_))
}
