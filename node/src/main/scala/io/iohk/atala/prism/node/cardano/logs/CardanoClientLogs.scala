package io.iohk.atala.prism.node.cardano.logs

import cats.syntax.apply._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import io.iohk.atala.prism.node.models.{TransactionDetails, TransactionId}
import io.iohk.atala.prism.node.cardano.CardanoClient
import io.iohk.atala.prism.node.cardano.models._
import io.iohk.atala.prism.node.models.WalletDetails
import tofu.higherKind.Mid
import tofu.logging.ServiceLogging
import tofu.syntax.logging._
import cats.MonadThrow

private[cardano] final class CardanoClientLogs[
    F[_]: ServiceLogging[*[_], CardanoClient[F]]: MonadThrow
] extends CardanoClient[Mid[F, *]] {
  override def getFullBlock(
      blockNo: Int
  ): Mid[F, Either[BlockError.NotFound, Block.Full]] =
    in =>
      info"getting full block $blockNo" *> in
        .flatTap(
          _.fold(
            err => error"Encountered an error while getting full block $blockNo: $err",
            block => info"getting full block with header ${block.header} - successfully done"
          )
        )
        .onError(errorCause"Encountered an error while getting full block" (_))

  override def getLatestBlock: Mid[F, Either[BlockError.NoneAvailable.type, Block.Canonical]] =
    in =>
      info"getting latest block" *> in
        .flatTap(
          _.fold(
            err => error"Encountered an error while getting latest block $err",
            block => info"getting latest block with header ${block.header} - successfully done"
          )
        )
        .onError(
          errorCause"Encountered an error while getting latest block" (_)
        )

  override def postTransaction(
      walletId: WalletId,
      payments: List[Payment],
      metadata: Option[TransactionMetadata],
      passphrase: String
  ): Mid[F, Either[CardanoWalletError, TransactionId]] =
    in =>
      info"posting transaction to wallet $walletId" *> in
        .flatTap(
          _.fold(
            err => error"Encountered an error while posting transaction to wallet $walletId: $err",
            id => info"posting transaction $id to wallet $walletId - successfully done"
          )
        )
        .onError(errorCause"Encountered an error while posting transaction" (_))

  override def getTransaction(
      walletId: WalletId,
      transactionId: TransactionId
  ): Mid[F, Either[CardanoWalletError, TransactionDetails]] =
    in =>
      info"getting transaction $transactionId" *> in
        .flatTap(
          _.fold(
            err => error"Encountered an error while getting transaction $transactionId: $err",
            transactionDetails => info"getting transaction $transactionDetails - successfully done"
          )
        )
        .onError(errorCause"Encountered an error while getting transaction" (_))

  override def deleteTransaction(
      walletId: WalletId,
      transactionId: TransactionId
  ): Mid[F, Either[CardanoWalletError, Unit]] =
    in =>
      info"deleting transaction $transactionId" *> in
        .flatTap(
          _.fold(
            err => error"Encountered an error while deleting transaction $transactionId: $err",
            _ => info"deleting transaction $transactionId - successfully done"
          )
        )
        .onError(
          errorCause"Encountered an error while deleting transaction" (_)
        )

  override def getWalletDetails(
      walletId: WalletId
  ): Mid[F, Either[CardanoWalletError, WalletDetails]] =
    in =>
      info"getting wallet $walletId details" *> in
        .flatTap(
          _.fold(
            err => error"Encountered an error while getting wallet $walletId details: $err",
            details => info"getting wallet $walletId details - successfully done $details"
          )
        )
        .onError(
          errorCause"Encountered an error while getting wallet details" (_)
        )
}
