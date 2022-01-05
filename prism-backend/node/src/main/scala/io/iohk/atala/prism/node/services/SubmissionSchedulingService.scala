package io.iohk.atala.prism.node.services

import cats.Id
import cats.effect.IO
import cats.effect.unsafe.IORuntime
import cats.syntax.functor._
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.node.services.SubmissionSchedulingService.Config
import io.iohk.atala.prism.tracing.Tracing._

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}

/** Scheduler which calls submitReceivedObjects and retryOldPendingTransactions periodically.
  *
  * @param config
  *   configuration of waiting timeouts between submissions and retries
  * @param submissionService
  *   service which implements submitReceivedObjects & retryOldPendingTransactions methods
  */
class SubmissionSchedulingService private (
    config: Config,
    submissionService: SubmissionService[IOWithTraceIdContext]
)(implicit ec: ExecutionContext, runtime: IORuntime) {
  type CancelToken = () => Future[Unit]

  // Schedule first run
  // NOTE: retryOldPendingTransactions is not thread-safe, so race-conditions may occur in a concurrent mode.
  scheduleRefreshTransactionStatuses(config.refreshTransactionStatusesPeriod)

  // NOTE: submitReceivedObjects is not thread-safe, so race-conditions may occur in a concurrent mode.
  scheduleSubmitReceivedObjects(config.operationSubmissionPeriod)

  // Every `delay` units of time, calls submissionService.retryOldPendingTransactions
  private def scheduleRefreshTransactionStatuses(
      delay: FiniteDuration
  ): Unit = trace[Id, Unit] { traceId =>
    (IO.sleep(delay) *> IO(
      // Ensure run is scheduled after completion, even if current run fails
      submissionService
        .refreshTransactionStatuses()
        .run(traceId)
        .unsafeToFuture()
        .onComplete { _ =>
          scheduleRefreshTransactionStatuses(config.refreshTransactionStatusesPeriod)
        }
    )).unsafeRunAndForget()
  }

  // Every delay calls submissionService.submitReceivedObjects
  // if immediate is set, then call submissionService.submitReceivedObjects without waiting
  private def scheduleSubmitReceivedObjects(delay: FiniteDuration): Unit = {
    def run(): Unit = trace { traceId =>
      // Ensure run is scheduled after completion, even if current run fails
      submissionService
        .submitReceivedObjects()
        .run(traceId)
        .unsafeToFuture()
    }.void
      .onComplete { _ =>
        scheduleSubmitReceivedObjects(config.operationSubmissionPeriod)
      }

    (IO.sleep(delay) *> IO(run())).unsafeRunAndForget()
  }
}

object SubmissionSchedulingService {
  case class Config(
      refreshTransactionStatusesPeriod: FiniteDuration = 20.seconds,
      operationSubmissionPeriod: FiniteDuration = 20.seconds
  )

  def apply(
      config: Config,
      submissionService: SubmissionService[IOWithTraceIdContext]
  )(implicit ec: ExecutionContext, runtime: IORuntime): SubmissionSchedulingService = {
    new SubmissionSchedulingService(config, submissionService)
  }
}
