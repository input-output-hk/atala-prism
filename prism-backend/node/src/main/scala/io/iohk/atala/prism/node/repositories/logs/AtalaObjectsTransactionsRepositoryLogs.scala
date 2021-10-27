package io.iohk.atala.prism.node.repositories.logs

import cats.effect.MonadThrow
import cats.syntax.apply._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import io.iohk.atala.prism.models.{Ledger, TransactionId}
import io.iohk.atala.prism.node.PublicationInfo
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.models.{
  AtalaObjectInfo,
  AtalaObjectTransactionSubmission,
  AtalaObjectTransactionSubmissionStatus
}
import io.iohk.atala.prism.node.repositories.AtalaObjectsTransactionsRepository
import io.iohk.atala.prism.node.services.models.AtalaObjectNotification
import tofu.higherKind.Mid
import tofu.logging.ServiceLogging
import tofu.syntax.logging._

import java.time.Duration

private[repositories] final class AtalaObjectsTransactionsRepositoryLogs[F[
    _
]: MonadThrow: ServiceLogging[*[
  _
], AtalaObjectsTransactionsRepository[F]]]
    extends AtalaObjectsTransactionsRepository[Mid[F, *]] {

  def retrieveObjects(
      transactions: List[AtalaObjectTransactionSubmission]
  ): Mid[F, List[Option[AtalaObjectInfo]]] =
    in =>
      info"retrieving objects, transactions size - ${transactions.size}" *> in
        .flatTap(result => info"retrieving objects - successfully done, got ${result.count(_.isDefined)} entities")
        .onError(errorCause"Encountered an error while retrieving objects" (_))

  def getOldPendingTransactions(
      ledgerPendingTransactionTimeout: Duration,
      ledger: Ledger
  ): Mid[F, List[AtalaObjectTransactionSubmission]] =
    in =>
      info"getting old pending transactions ${ledgerPendingTransactionTimeout.toString} ${ledger.entryName}" *> in
        .flatTap(result => info"getting old pending transactions - successfully done, got ${result.size} entities")
        .onError(
          errorCause"Encountered an error while getting old pending transactions" (
            _
          )
        )

  def getNotPublishedObjects: Mid[F, Either[NodeError, List[AtalaObjectInfo]]] =
    in =>
      info"getting not published objects" *> in
        .flatTap(
          _.fold(
            err => error"Encountered an error while getting not published objects $err",
            list => info"getting not published objects - successfully done, got ${list.size} entities"
          )
        )
        .onError(
          errorCause"Encountered an error while getting not published objects" (
            _
          )
        )

  def updateSubmissionStatus(
      submission: AtalaObjectTransactionSubmission,
      newSubmissionStatus: AtalaObjectTransactionSubmissionStatus
  ): Mid[F, Either[NodeError, Unit]] =
    in =>
      info"""updating submission status ${submission.transactionId} old status - ${submission.status.entryName}
            new status - ${newSubmissionStatus.entryName} ${submission.ledger.entryName}""" *> in
        .flatTap(
          _.fold(
            err => error"Encountered an error while updating submission status $err",
            _ => info"updating submission status - successfully done"
          )
        )
        .onError(
          errorCause"Encountered an error while updating submission status" (_)
        )

  override def updateSubmissionStatusIfExists(
      ledger: Ledger,
      transactionId: TransactionId,
      newSubmissionStatus: AtalaObjectTransactionSubmissionStatus
  ): Mid[F, Either[NodeError, Unit]] =
    in =>
      info"updating submission status new status - ${newSubmissionStatus.entryName}, ${ledger.entryName}" *> in
        .flatTap(
          _.fold(
            err => error"Encountered an error while updating submission status $err",
            _ => info"updating submission status - successfully done"
          )
        )
        .onError(
          errorCause"Encountered an error while updating submission status" (_)
        )

  def storeTransactionSubmission(
      atalaObjectInfo: AtalaObjectInfo,
      publication: PublicationInfo
  ): Mid[F, Either[NodeError, AtalaObjectTransactionSubmission]] =
    in =>
      info"storing transaction submission - ${atalaObjectInfo.objectId.toString}" *> in
        .flatTap(
          _.fold(
            err => error"Encountered an error while storing transaction submission $err",
            result =>
              info"storing transaction submission - successfully done ${result.ledger.entryName} ${result.transactionId}"
          )
        )
        .onError(
          errorCause"Encountered an error while storing transaction submission" (
            _
          )
        )

  def setObjectTransactionDetails(
      notification: AtalaObjectNotification
  ): Mid[F, Option[AtalaObjectInfo]] =
    in =>
      info"setting object transaction details - ${notification.transaction.transactionId} ${notification.transaction.ledger.entryName}" *> in
        .flatTap(
          _.fold(error"setting object transaction details - got nothing")(obj =>
            info"setting object transaction details - successfully done ${obj.objectId}"
          )
        )
        .onError(
          errorCause"Encountered an error while setting object transaction details" (
            _
          )
        )
}
