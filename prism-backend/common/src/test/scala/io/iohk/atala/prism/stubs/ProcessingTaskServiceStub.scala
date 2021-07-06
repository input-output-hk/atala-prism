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

class ProcessingTaskServiceStub[S <: ProcessingTaskState]() extends ProcessingTaskService[S] {

  val createTaskInvokeCount = new AtomicInteger(0)
  val extendLeaseInvokeCount = new AtomicInteger(0)
  val scheduleTaskInvokeCount = new AtomicInteger(0)
  val updateTaskAndExtendLeaseInvokeCount = new AtomicInteger(0)
  val deleteInvokeCount = new AtomicInteger(0)

  var callbackOption: Option[() => Unit] = None

  def registerNotifyIdleWorkerCallback(callback: () => Unit): Unit = {
    callbackOption = Some(callback)
  }

  def create(
      processingTaskData: ProcessingTaskData,
      processingTaskState: S,
      scheduledTime: Instant
  ): Task[ProcessingTaskId] = {
    createTaskInvokeCount.incrementAndGet()
    Task.pure(ProcessingTaskId.random()).map { id =>
      if (!scheduledTime.isAfter(Instant.now())) {
        callbackOption.foreach(callback => callback())
      }
      id
    }
  }

  def fetchTaskToProcess(leaseTimeSeconds: Int, workerNumber: Int): Task[Option[ProcessingTask[S]]] = Task.pure(None)

  def ejectTask(
      _processingTaskId: ProcessingTaskId,
      _workerNumber: Int,
      _leaseTimeSeconds: Int
  ): Task[Option[ProcessingTask[S]]] =
    Task.pure(None)

  def extendLease(processingTaskId: ProcessingTaskId, workerNumber: Int, leaseTimeSeconds: Int): Task[Unit] = {
    extendLeaseInvokeCount.incrementAndGet()
    Task.unit
  }

  def updateData(
      processingTaskId: ProcessingTaskId,
      _workerNumber: Int,
      data: ProcessingTaskData
  ): Task[Unit] = Task.unit

  def scheduleTask(
      processingTaskId: ProcessingTaskId,
      workerNumber: Int,
      state: S,
      data: ProcessingTaskData,
      scheduledTime: Instant
  ): Task[Unit] = {
    scheduleTaskInvokeCount.incrementAndGet()
    Task.unit
  }

  def updateTaskAndExtendLease(
      processingTaskId: ProcessingTaskId,
      workerNumber: Int,
      state: S,
      data: ProcessingTaskData,
      leaseTimeSeconds: Int
  ): Task[ProcessingTask[S]] = {
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

  def deleteTask(processingTaskId: ProcessingTaskId, workerNumber: Int): Task[Unit] = {
    deleteInvokeCount.incrementAndGet()
    Task.unit
  }

}
