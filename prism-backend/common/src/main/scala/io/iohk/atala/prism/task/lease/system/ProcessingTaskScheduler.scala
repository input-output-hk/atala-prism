package io.iohk.atala.prism.task.lease.system

import io.iohk.atala.prism.task.lease.system.ProcessingTaskLeaseConfig
import monix.eval.Task
import monix.execution.AsyncSemaphore
import org.slf4j.LoggerFactory

import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.duration.DurationInt
import cats.implicits._

class ProcessingTaskScheduler[S <: ProcessingTaskState](
    processingTaskService: ProcessingTaskService[S],
    processingTaskRouter: ProcessingTaskRouter[S],
    taskLeaseConfig: ProcessingTaskLeaseConfig
) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  private val semaphore = AsyncSemaphore(provisioned = 0.toLong)
  private val idleWorkersNumber = new AtomicInteger(0)

  def run: Task[IndexedSeq[Unit]] = {
    processingTaskService.registerNotifyIdleWorkerCallback(() => notifyIdleWorker())
    Task.parSequence(1.to(taskLeaseConfig.numberOfWorkers).map(runWorker))
  }

  private[task] def notifyIdleWorker(): Unit = {
    // This is not 100% correct. It may occur that semaphore is released even though idleWorkersNumber is 0.
    // Ideally, this whole section and code which increments and decrements counter should
    // be guarded by mutex, but in my opinion that would cause unnecessary performance bottleneck.
    // In the worst case, semaphore will be realesed when there is no free worker and after that,
    // when there is a free worker it will not wait required time (it will acquire semaphore immediately).
    if (idleWorkersNumber.get() > 0) {
      semaphore.release()
    }
  }

  private[task] def runWorker(workerNumber: Int): Task[Unit] = {
    Task.tailRecM(()) { _ =>
      processingTaskService
        .fetchTaskToProcess(taskLeaseConfig.leaseTimeSeconds)
        .flatMap {
          case Some(processingTask) => process(processingTask, workerNumber)
          case None =>
            logger.debug(s"Worker: $workerNumber, no task to process")
            idleWorkersNumber.incrementAndGet()
            Task
              .race(Task.fromFuture(semaphore.acquire()), Task.sleep(taskLeaseConfig.workerSleepTimeSeconds.seconds))
              .map(_ => idleWorkersNumber.decrementAndGet())
        }
        .map(_ => Left(()))
    }
  }

  private[task] def process(processingTask: ProcessingTask[S], workerNumber: Int): Task[Unit] = {
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
          case Right(ProcessingTaskResult.ProcessingTaskRestart) =>
            logger.warn(
              s"Worker: ${workerNumber}, ProcessingTask: ${task.id} with state: ${task.state} finished, although it shouldn't. Restarting it."
            )
            processingTaskService
              .updateTaskAndExtendLease(task.id, task.state, task.data, taskLeaseConfig.leaseTimeSeconds)
              .map(Left(_))
          case Left(_) =>
            logger.warn(
              s"Worker: $workerNumber, error occurred when processing task with id: ${task.id}, extend lease task unexpectedly finished"
            )
            Task.pure(Right(()))
        }
        .onErrorHandle(ex => {
          logger.warn(
            s"Worker: $workerNumber, error occurred when processing task with id: ${task.id}, error: ${ex.getMessage}"
          )
          Right(())
        })
    }
  }

}
