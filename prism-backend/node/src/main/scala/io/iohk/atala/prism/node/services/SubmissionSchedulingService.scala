package io.iohk.atala.prism.node.services

import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.node.services.SubmissionSchedulingService.Config
import monix.execution.Scheduler
import org.slf4j.LoggerFactory

import java.time.Duration
import scala.concurrent.duration.{DurationInt, FiniteDuration}

class SubmissionSchedulingService private (
    config: Config,
    submissionService: SubmissionService[IOWithTraceIdContext]
)(implicit scheduler: Scheduler) {
  private val logger = LoggerFactory.getLogger(this.getClass)

  // Schedule first run
  // NOTE: retryOldPendingTransactions is not thread-safe, so race-conditions may occur in a concurrent mode.
  scheduleRetryOldPendingTransactions(config.transactionRetryPeriod)

  // NOTE: submitReceivedObjects is not thread-safe, so race-conditions may occur in a concurrent mode.
  private var submitReceivedObjectsTask: Option[monix.execution.Cancelable] =
    None
  scheduleSubmitReceivedObjects(config.operationSubmissionPeriod)

  def flushOperationsBuffer(): Unit = {
    submitReceivedObjectsTask.fold(
      logger.info(
        "Skip flushing because operations submission is already in progress."
      )
    ) { task =>
      task.cancel() // cancel a scheduled task
      scheduleSubmitReceivedObjects(
        config.operationSubmissionPeriod,
        immediate = true
      )
    }
  }

  private def scheduleRetryOldPendingTransactions(
      delay: FiniteDuration
  ): Unit = {
    scheduler.scheduleOnce(delay) {
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
    }
    ()
  }

  private def scheduleSubmitReceivedObjects(
      delay: FiniteDuration,
      immediate: Boolean = false
  ): Unit = {
    def run(): Unit = {
      submitReceivedObjectsTask = None
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
      submitReceivedObjectsTask = Some(
        scheduler.scheduleOnce(delay)(run())
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
  )(implicit
      scheduler: Scheduler
  ): SubmissionSchedulingService = {
    new SubmissionSchedulingService(config, submissionService)
  }
}
