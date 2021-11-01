package io.iohk.atala.prism.node.services

import cats.effect.{CancelToken, IO, Timer}
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.node.services.SubmissionSchedulingService.Config
import org.slf4j.LoggerFactory

import java.time.Duration
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{DurationInt, FiniteDuration}

class SubmissionSchedulingService private (
    config: Config,
    submissionService: SubmissionService[IOWithTraceIdContext]
)(implicit ec: ExecutionContext) {
  private implicit val timer: Timer[IO] = IO.timer(ec)

  private val logger = LoggerFactory.getLogger(this.getClass)

  // Schedule first run
  // NOTE: retryOldPendingTransactions is not thread-safe, so race-conditions may occur in a concurrent mode.
  scheduleRetryOldPendingTransactions(config.transactionRetryPeriod)

  // NOTE: submitReceivedObjects is not thread-safe, so race-conditions may occur in a concurrent mode.
  private var submitReceivedObjectsCancellationToken: Option[CancelToken[IO]] =
    None
  scheduleSubmitReceivedObjects(config.operationSubmissionPeriod)

  def flushOperationsBuffer(): Unit = {
    submitReceivedObjectsCancellationToken.fold(
      logger.info(
        "Skip flushing because operations submission is already in progress."
      )
    ) { cancellationToken =>
      cancellationToken.unsafeRunSync() // cancel a scheduled task
      scheduleSubmitReceivedObjects(
        config.operationSubmissionPeriod,
        immediate = true
      )
    }
  }

  private def scheduleRetryOldPendingTransactions(
      delay: FiniteDuration
  ): Unit = {
    (IO.sleep(delay) *> IO(
      // Ensure run is scheduled after completion, even if current run fails
      submissionService
        .retryOldPendingTransactions(config.ledgerPendingTransactionTimeout)
        .run(TraceId.generateYOLO)
        .unsafeToFuture()
        .recover { err =>
          logger.error("Could not retry old pending transactions", err)
        }
        .onComplete { _ =>
          scheduleRetryOldPendingTransactions(config.transactionRetryPeriod)
        }
    )).unsafeRunAsyncAndForget()
  }

  private def scheduleSubmitReceivedObjects(
      delay: FiniteDuration,
      immediate: Boolean = false
  ): Unit = {
    def run(): Unit = {
      submitReceivedObjectsCancellationToken = None
      // Ensure run is scheduled after completion, even if current run fails
      submissionService
        .submitReceivedObjects()
        .run(TraceId.generateYOLO)
        .unsafeToFuture()
        .map { submissionResult =>
          submissionResult.left.foreach { err =>
            logger.error("Could not submit received objects", err)
          }
          ()
        }
        .onComplete { _ =>
          scheduleSubmitReceivedObjects(config.operationSubmissionPeriod)
        }
    }

    if (immediate) {
      run()
    } else {
      submitReceivedObjectsCancellationToken = Some(
        (IO.sleep(delay) *> IO(run()))
          .unsafeRunCancelable(_ => ())
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
  )(implicit ec: ExecutionContext): SubmissionSchedulingService = {
    new SubmissionSchedulingService(config, submissionService)
  }
}
