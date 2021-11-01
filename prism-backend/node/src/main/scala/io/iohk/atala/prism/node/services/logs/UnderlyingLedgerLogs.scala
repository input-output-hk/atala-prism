package io.iohk.atala.prism.node.services.logs

import cats.effect.MonadThrow
import cats.syntax.applicativeError._
import cats.syntax.apply._
import cats.syntax.flatMap._
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.models.{Ledger, TransactionDetails, TransactionId}
import io.iohk.atala.prism.node.cardano.models.CardanoWalletError
import io.iohk.atala.prism.node.{PublicationInfo, UnderlyingLedger}
import io.iohk.atala.prism.protos.node_internal.AtalaObject
import tofu.higherKind.Mid
import tofu.logging.ServiceLogging
import tofu.syntax.logging._

class UnderlyingLedgerLogs[
    F[_]: ServiceLogging[*[_], UnderlyingLedger[F]]: MonadThrow
] extends UnderlyingLedger[Mid[F, *]] {

  // wont be logged/called since not mid
  override def getType: Ledger = ???

  override def publish(
      obj: AtalaObject
  ): Mid[F, Either[CardanoWalletError, PublicationInfo]] =
    in =>
      info"publishing object with - ${obj.blockContent.map(_.operations.size)} operations" *> in
        .flatTap(
          _.fold(
            er => error"Encountered an error while publishing object $er ${getOperationsIds(obj)}",
            result => info"publishing object - successfully done ${result.transaction.transactionId}"
          )
        )
        .onError(errorCause"Encountered an error while publishing object" (_))

  override def getTransactionDetails(
      transactionId: TransactionId
  ): Mid[F, Either[CardanoWalletError, TransactionDetails]] =
    in =>
      info"getting transaction details $transactionId" *> in
        .flatTap(
          _.fold(
            er => error"Encountered an error while getting transaction details $er",
            result => info"getting transaction details - successfully done ${result.status.entryName}"
          )
        )
        .onError(
          errorCause"Encountered an error while getting transaction details" (_)
        )

  override def deleteTransaction(
      transactionId: TransactionId
  ): Mid[F, Either[CardanoWalletError, Unit]] =
    in =>
      info"deleting transaction $transactionId" *> in
        .flatTap(
          _.fold(
            er => error"Encountered an error while deleting transaction $er",
            _ => info"deleting transaction - successfully done"
          )
        )
        .onError(
          errorCause"Encountered an error while deleting transaction" (_)
        )

  def getOperationsIds(obj: AtalaObject): List[AtalaOperationId] = {
    obj.blockContent
      .map { block =>
        block.operations.toList.map(AtalaOperationId.of)
      }
      .toList
      .flatten
  }
}
