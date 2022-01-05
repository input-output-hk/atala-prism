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

private[services] final class SubmissionServiceLogs[
    F[_]: ServiceLogging[*[_], SubmissionService[F]]: MonadThrow
] extends SubmissionService[Mid[F, *]] {
  override def submitReceivedObjects(): Mid[F, Either[errors.NodeError, Int]] =
    in =>
      info"submitting received objects" *> in
        .flatTap(
          _.fold(
            err => error"Encountered an error while submitting received objects $err",
            publishedTransactionsNumber =>
              info"submitting received objects - successfully done, published $publishedTransactionsNumber transactions"
          )
        )
        .onError(
          errorCause"Encountered an error while submitting received objects" (_)
        )

  override def refreshTransactionStatuses(): Mid[F, RefreshTransactionStatusesResult] =
    in =>
      info"refreshing transactions statuses" *> in
        .flatTap(result => info"refreshing transactions statuses - successfully done $result")
        .onError(
          errorCause"Encountered an error while refreshing transactions statuses" (
            _
          )
        )
}
