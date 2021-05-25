package io.iohk.atala.prism.task.lease.system

import io.iohk.atala.prism.task.lease.system.ProcessingTaskLeaseConfig
import monix.eval.Task
import org.slf4j.LoggerFactory
import scala.concurrent.duration.DurationInt

class ProcessingTaskScheduler(
    processingTaskService: ProcessingTaskService,
    processingTaskRouter: ProcessingTaskRouter,
    taskLeaseConfig: ProcessingTaskLeaseConfig
) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def run: Task[IndexedSeq[Unit]] = {
    Task.parSequence(1.to(taskLeaseConfig.numberOfWorkers).map(runWorker))
  }

  private[task] def runWorker(workerNumber: Int): Task[Unit] = {
    Task.tailRecM(()) { _ =>
      processingTaskService
        .fetchTaskToProcess(taskLeaseConfig.leaseTimeSeconds)
        .flatMap {
          case Some(processingTask) => process(processingTask, workerNumber)
          case None =>
            logger.debug(s"Worker: $workerNumber, no task to process")
            Task.sleep(taskLeaseConfig.workerSleepTimeSeconds.seconds)
        }
        .map(_ => Left(()))
    }
  }

  private[task] def process(processingTask: ProcessingTask, workerNumber: Int): Task[Unit] = {
    Task.tailRecM(processingTask) { task =>
      logger.info(s"Worker: $workerNumber, processing task with id: ${task.id}, state: ${task.state}")

      val extendLeaseTask = Task.tailRecM(()) { _ =>
        Task
          .sleep(taskLeaseConfig.extendLeaseTimeIntervalSeconds.seconds)
          .flatMap(_ => processingTaskService.extendLease(task.id, taskLeaseConfig.leaseTimeSeconds))
          .map(_ => Left(()))
      }

      Task
        .race(extendLeaseTask, processingTaskRouter.process(task))
        .flatMap {
          case Right(ProcessingTaskResult.ProcessingTaskFinished) =>
            processingTaskService
              .deleteTask(task.id)
              .map(_ => Right(()))
          case Right(ProcessingTaskResult.ProcessingTaskScheduled(state, data, scheduledTime)) =>
            processingTaskService
              .scheduleTask(task.id, state, data, scheduledTime)
              .map(_ => Right(()))
          case Right(ProcessingTaskResult.ProcessingTaskStateTransition(state, data)) =>
            processingTaskService
              .updateTaskAndExtendLease(task.id, state, data, taskLeaseConfig.leaseTimeSeconds)
              .map(Left(_))
          case Left(_) =>
            logger.warn(
              s"Worker: $workerNumber, error occurred when processing task with id: ${task.id}, extend lease task unexpectedly finished"
            )
            Task.pure(Right(()))
        }
        .redeem(
          ex => {
            logger.warn(
              s"Worker: $workerNumber, error occurred when processing task with id: ${task.id}, error: ${ex.getMessage}"
            )
            Right(())
          },
          id => id
        )
    }
  }

}
