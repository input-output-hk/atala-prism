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

/** Scheduler which updates statuses and publishes new transactions periodically.
  *
  * @param config
  *   configuration of waiting timeouts between submissions and updates
  * @param submissionService
  *   service which implements refreshTransactionStatuses, submitPendingObjects & scheduledObjectsToPending methods
  */
class SubmissionSchedulingService private (
    config: Config,
    submissionService: SubmissionService[IOWithTraceIdContext]
)(implicit ec: ExecutionContext, runtime: IORuntime) {
  type CancelToken = () => Future[Unit]

  // Schedule first run
  // NOTE: refreshAndSubmit is not thread-safe, so race-conditions may occur in a concurrent mode.
  scheduleRefreshAndSubmit(config.refreshAndSubmitPeriod)

  scheduleMoveScheduledToPending(config.moveScheduledToPendingPeriod)

  // Every `delay` units of time, calls refreshTransactionStatuses and then submitPendingObjects
  private def scheduleRefreshAndSubmit(
      delay: FiniteDuration
  ): Unit = trace[Id, Unit] { traceId =>
    val refreshAndSubmitQuery = for {
      _ <- submissionService.refreshTransactionStatuses()
      _ <- submissionService.submitPendingObjects()
    } yield ()

    (IO.sleep(delay) *> IO(
      // Ensure run is scheduled after completion, even if current run fails
      refreshAndSubmitQuery
        .run(traceId)
        .unsafeToFuture()
        .onComplete { _ =>
          scheduleRefreshAndSubmit(config.refreshAndSubmitPeriod)
        }
    )).unsafeRunAndForget()
  }

  // Every delay calls submissionService.submitReceivedObjects
  // if immediate is set, then call submissionService.submitReceivedObjects without waiting
  private def scheduleMoveScheduledToPending(delay: FiniteDuration): Unit = {
    def run(): Unit = trace { traceId =>
      // Ensure run is scheduled after completion, even if current run fails
      submissionService.scheduledObjectsToPending
        .run(traceId)
        .unsafeToFuture()
    }.void
      .onComplete { _ =>
        scheduleMoveScheduledToPending(config.moveScheduledToPendingPeriod)
      }

    (IO.sleep(delay) *> IO(run())).unsafeRunAndForget()
  }
}

object SubmissionSchedulingService {
  case class Config(
      refreshAndSubmitPeriod: FiniteDuration = 20.seconds,
      moveScheduledToPendingPeriod: FiniteDuration = 20.seconds
  )

  def apply(
      config: Config,
      submissionService: SubmissionService[IOWithTraceIdContext]
  )(implicit ec: ExecutionContext, runtime: IORuntime): SubmissionSchedulingService = {
    new SubmissionSchedulingService(config, submissionService)
  }
}
