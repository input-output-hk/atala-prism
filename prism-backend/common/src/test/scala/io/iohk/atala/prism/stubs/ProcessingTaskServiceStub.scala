package io.iohk.atala.prism.stubs

import io.iohk.atala.prism.task.lease.system.{
  ProcessingTask,
  ProcessingTaskData,
  ProcessingTaskId,
  ProcessingTaskService,
  ProcessingTaskState
}
import monix.eval.Task

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicInteger

class ProcessingTaskServiceStub() extends ProcessingTaskService {

  val extendLeaseInvokeCount = new AtomicInteger(0)
  val scheduleTaskInvokeCount = new AtomicInteger(0)
  val updateTaskAndExtendLeaseInvokeCount = new AtomicInteger(0)
  val deleteInvokeCount = new AtomicInteger(0)

  def create(
      processingTaskData: ProcessingTaskData,
      processingTaskState: ProcessingTaskState,
      scheduledTime: Instant
  ): Task[ProcessingTaskId] = Task.pure(ProcessingTaskId.random())

  def fetchTaskToProcess(leaseTimeSeconds: Int): Task[Option[ProcessingTask]] = Task.pure(None)

  def ejectTask(processingTaskId: ProcessingTaskId, leaseTimeSeconds: Int): Task[Option[ProcessingTask]] =
    Task.pure(None)

  def extendLease(processingTaskId: ProcessingTaskId, leaseTimeSeconds: Int): Task[Unit] = {
    extendLeaseInvokeCount.incrementAndGet()
    Task.unit
  }

  def updateData(
      processingTaskId: ProcessingTaskId,
      data: ProcessingTaskData
  ): Task[Unit] = Task.unit

  def scheduleTask(
      processingTaskId: ProcessingTaskId,
      state: ProcessingTaskState,
      data: ProcessingTaskData,
      scheduledTime: Instant
  ): Task[Unit] = {
    scheduleTaskInvokeCount.incrementAndGet()
    Task.unit
  }

  def updateTaskAndExtendLease(
      processingTaskId: ProcessingTaskId,
      state: ProcessingTaskState,
      data: ProcessingTaskData,
      leaseTimeSeconds: Int
  ): Task[ProcessingTask] = {
    updateTaskAndExtendLeaseInvokeCount.incrementAndGet()
    Task.pure(
      ProcessingTask(
        id = processingTaskId,
        state = state,
        owner = None,
        data = data,
        lastChange = Instant.now(),
        nextAction = Instant.now().plus(leaseTimeSeconds.toLong, ChronoUnit.SECONDS)
      )
    )
  }

  def deleteTask(processingTaskId: ProcessingTaskId): Task[Unit] = {
    deleteInvokeCount.incrementAndGet()
    Task.unit
  }

}
