package io.iohk.atala.prism.node.services.logs

import cats.effect.MonadThrow
import cats.syntax.apply._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import io.iohk.atala.prism.node.errors
import io.iohk.atala.prism.node.services.SubmissionService
import tofu.higherKind.Mid
import tofu.logging.ServiceLogging
import tofu.syntax.logging._

import java.time.Duration

private[services] final class SubmissionServiceLogs[
    F[_]: ServiceLogging[*[_], SubmissionService[F]]: MonadThrow
] extends SubmissionService[Mid[F, *]] {
  override def submitReceivedObjects(): Mid[F, Either[errors.NodeError, Unit]] =
    in =>
      info"submitting received objects" *> in
        .flatTap(
          _.fold(
            err => error"Encountered an error while submitting received objects $err",
            _ => info"submitting received objects - successfully done"
          )
        )
        .onError(
          errorCause"Encountered an error while submitting received objects" (_)
        )

  override def retryOldPendingTransactions(
      ledgerPendingTransactionTimeout: Duration
  ): Mid[F, Int] =
    in =>
      info"retrying old pending transactions, duration $ledgerPendingTransactionTimeout" *> in
        .flatTap(_ => info"retrying old pending transactions - successfully done")
        .onError(
          errorCause"Encountered an error while retrying old pending transactions" (
            _
          )
        )
}
