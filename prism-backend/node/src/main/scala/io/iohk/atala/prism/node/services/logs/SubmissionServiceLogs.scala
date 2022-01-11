package io.iohk.atala.prism.node.services.logs

import cats.syntax.apply._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import io.iohk.atala.prism.node.errors
import io.iohk.atala.prism.node.services.SubmissionService
import io.iohk.atala.prism.node.services.models.RefreshTransactionStatusesResult
import tofu.higherKind.Mid
import tofu.logging.ServiceLogging
import tofu.syntax.logging._
import cats.MonadThrow
import io.iohk.atala.prism.node.errors.NodeError

private[services] final class SubmissionServiceLogs[
    F[_]: ServiceLogging[*[_], SubmissionService[F]]: MonadThrow
] extends SubmissionService[Mid[F, *]] {
  override def submitPendingObjects(): Mid[F, Either[errors.NodeError, Int]] = {
    val description = "submitting pending objects"
    in =>
      info"$description" *> in
        .flatTap(
          _.fold(
            err => error"Encountered an error while $description $err",
            publishedTransactionsNumber =>
              info"$description - successfully done, $publishedTransactionsNumber transactions were created in Cardano wallet"
          )
        )
        .onError(
          errorCause"Encountered an error while $description" (_)
        )
  }

  override def refreshTransactionStatuses(): Mid[F, RefreshTransactionStatusesResult] = {
    val description = "refreshing transactions statuses"
    in =>
      info"$description" *> in
        .flatTap(result => info"$description - successfully done $result")
        .onError(
          errorCause"Encountered an error while $description" (
            _
          )
        )
  }

  override def scheduledObjectsToPending: Mid[F, Either[NodeError, Int]] = {
    val description = "move scheduled objects to pending status"
    in =>
      info"$description" *> in
        .flatTap(
          _.fold(
            err => error"Encountered an error while $description $err",
            numUpdated => info"$description - successfully updated $numUpdated objects"
          )
        )
        .onError(
          errorCause"Encountered an error while $description" (
            _
          )
        )
  }
}
