package io.iohk.atala.prism.node.services

import cats.Id
import cats.effect.IO
import cats.effect.unsafe.IORuntime
import cats.syntax.functor._
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.node.services.SubmissionSchedulingService.Config
import io.iohk.atala.prism.tracing.Tracing._
import org.slf4j.LoggerFactory

import java.time.Duration
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps

class SubmissionSchedulingService private (
    config: Config,
    submissionService: SubmissionService[IOWithTraceIdContext]
)(implicit ec: ExecutionContext, runtime: IORuntime) {
  type CancelToken = () => Future[Unit]
  private val logger = LoggerFactory.getLogger(this.getClass)

  // Schedule first run
  // NOTE: retryOldPendingTransactions is not thread-safe, so race-conditions may occur in a concurrent mode.
  scheduleRetryOldPendingTransactions(config.transactionRetryPeriod)

  // NOTE: submitReceivedObjects is not thread-safe, so race-conditions may occur in a concurrent mode.
  private var submitReceivedObjectsCancellationToken: Option[CancelToken] =
    None
  scheduleSubmitReceivedObjects(config.operationSubmissionPeriod)

  def flushOperationsBuffer(): Unit = {
    submitReceivedObjectsCancellationToken.fold(
      logger.info(
        "Skip flushing because operations submission is already in progress."
      )
    ) { cancellationToken =>
      Await.result(cancellationToken(), 5 seconds) // cancel a scheduled task
      scheduleSubmitReceivedObjects(
        config.operationSubmissionPeriod,
        immediate = true
      )
    }
  }

  // Every `delay` units of time, calls submissionService.retryOldPendingTransactions
  private def scheduleRetryOldPendingTransactions(
      delay: FiniteDuration
  ): Unit = trace[Id, Unit] { traceId =>
    (IO.sleep(delay) *> IO(
      // Ensure run is scheduled after completion, even if current run fails
      submissionService
        .retryOldPendingTransactions(config.ledgerPendingTransactionTimeout)
        .run(traceId)
        .unsafeToFuture()
        .onComplete { _ =>
          scheduleRetryOldPendingTransactions(config.transactionRetryPeriod)
        }
    )).unsafeRunAndForget()
  }

  // Every delay calls submissionService.submitReceivedObjects
  // if immediate is set, then call submissionService.submitReceivedObjects without waiting
  private def scheduleSubmitReceivedObjects(
      delay: FiniteDuration,
      immediate: Boolean = false
  ): Unit = {
    def run(): Unit = trace { traceId =>
      submitReceivedObjectsCancellationToken = None
      // Ensure run is scheduled after completion, even if current run fails
      submissionService
        .submitReceivedObjects()
        .run(traceId)
        .unsafeToFuture()
    }.void
      .onComplete { _ =>
        scheduleSubmitReceivedObjects(config.operationSubmissionPeriod)
      }

    if (immediate) {
      run()
    } else {
      submitReceivedObjectsCancellationToken = Some(
        (IO.sleep(delay) *> IO(run()))
          .unsafeRunCancelable()
      )
    }
    ()
  }
}

object SubmissionSchedulingService {
  case class Config(
      ledgerPendingTransactionTimeout: Duration,
      transactionRetryPeriod: FiniteDuration = 20.seconds,
      operationSubmissionPeriod: FiniteDuration = 20.seconds
  )

  def apply(
      config: Config,
      submissionService: SubmissionService[IOWithTraceIdContext]
  )(implicit ec: ExecutionContext, runtime: IORuntime): SubmissionSchedulingService = {
    new SubmissionSchedulingService(config, submissionService)
  }
}
