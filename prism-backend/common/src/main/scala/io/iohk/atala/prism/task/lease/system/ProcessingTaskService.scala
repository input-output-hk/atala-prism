package io.iohk.atala.prism.task.lease.system

import cats.data.OptionT
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.task.lease.system.ProcessingTaskDao
import monix.eval.Task
import doobie.implicits._

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import doobie.{ConnectionIO, FC}
import cats.implicits._

trait ProcessingTaskService[S <: ProcessingTaskState] {

  /** Registers a callback which will be called when a new processing task is created and it should be run immediately
    * (scheduledTime <= Instant.now()).
    */
  def registerNotifyIdleWorkerCallback(callback: () => Unit): Unit

  /** Creates a completely new processing task that will be processed after scheduled time.
    */
  def create(
      processingTaskData: ProcessingTaskData,
      processingTaskState: S,
      scheduledTime: Instant
  ): Task[ProcessingTaskId]

  /** Acquires a processing task for a specified period of time. Returns None when there is no task to process.
    */
  def fetchTaskToProcess(leaseTimeSeconds: Int, workerNumber: Int): Task[Option[ProcessingTask[S]]]

  /** Acquires tha task specified by id immediately, even though it's being processed by another instance which has a
    * valid lease. Returns None when task doesn't exist.
    */
  def ejectTask(
      processingTaskId: ProcessingTaskId,
      workerNumber: Int,
      leaseTimeSeconds: Int
  ): Task[Option[ProcessingTask[S]]]

  /** Extends a lese of the processing task by specified number of seconds. Fails when the processing task has been
    * ejected by another instance.
    */
  def extendLease(processingTaskId: ProcessingTaskId, workerNumber: Int, leaseTimeSeconds: Int): Task[Unit]

  /** Updates the processing task data. Fails when the processing task has been ejected by another instance.
    */
  def updateData(
      processingTaskId: ProcessingTaskId,
      workerNumber: Int,
      data: ProcessingTaskData
  ): Task[Unit]

  /** Schedules the processing task to be run after scheduled time at a new state and with new data. Clears the owner of
    * the task. Fails when the processing task has been ejected by another instance.
    */
  def scheduleTask(
      processingTaskId: ProcessingTaskId,
      workerNumber: Int,
      state: S,
      data: ProcessingTaskData,
      scheduledTime: Instant
  ): Task[Unit]

  /** Extends a lease of the processing and updates the processing task with a new state and data. Fails when the
    * processing task has been ejected by another instance.
    */
  def updateTaskAndExtendLease(
      processingTaskId: ProcessingTaskId,
      workerNumber: Int,
      state: S,
      data: ProcessingTaskData,
      leaseTimeSeconds: Int
  ): Task[ProcessingTask[S]]

  /** Deletes the processing task completely from database. Processing task can only be deleted when current instance
    * own it or it's not owned by any instance. Fails when the processing task has been ejected by another instance.
    */
  def deleteTask(processingTaskId: ProcessingTaskId, workerNumber: Int): Task[Unit]
}

class ProcessingTaskServiceImpl[S <: ProcessingTaskState](
    tx: Transactor[Task],
    currentInstanceUUID: UUID,
    processingTaskDao: ProcessingTaskDao[S]
) extends ProcessingTaskService[S] {

  var callbackOption: Option[() => Unit] = None

  def registerNotifyIdleWorkerCallback(callback: () => Unit): Unit = {
    callbackOption = Some(callback)
  }

  def workerId(workerNumber: Int) = ProcessingTaskOwner(s"$currentInstanceUUID-$workerNumber")

  override def create(
      processingTaskData: ProcessingTaskData,
      processingTaskState: S,
      scheduledTime: Instant
  ): Task[ProcessingTaskId] = {
    val processingTask = ProcessingTask[S](
      id = ProcessingTaskId.random(),
      state = processingTaskState,
      owner = None,
      lastChange = Instant.now(),
      nextAction = scheduledTime,
      data = processingTaskData
    )

    processingTaskDao.insert(processingTask).transact(tx).map(_ => processingTask.id).map { id =>
      if (!scheduledTime.isAfter(Instant.now())) {
        callbackOption.foreach(callback => callback())
      }
      id
    }
  }

  override def fetchTaskToProcess(leaseTimeSeconds: Int, workerNumber: Int): Task[Option[ProcessingTask[S]]] = {
    (for {
      taskToProcess <- OptionT(processingTaskDao.fetchTaskToProcess())
      updated = taskToProcess.copy(
        owner = Some(workerId(workerNumber)),
        nextAction = Instant.now().plus(leaseTimeSeconds.toLong, ChronoUnit.SECONDS)
      )
      _ <- OptionT.liftF(processingTaskDao.updateOwnerAndLease(updated))
    } yield updated).value
      .transact(tx)
  }

  override def ejectTask(
      processingTaskId: ProcessingTaskId,
      workerNumber: Int,
      leaseTimeSeconds: Int
  ): Task[Option[ProcessingTask[S]]] = {
    (for {
      task <- OptionT(processingTaskDao.findById(processingTaskId))
      updated = task.copy(
        owner = Some(workerId(workerNumber)),
        nextAction = Instant.now().plus(leaseTimeSeconds.toLong, ChronoUnit.SECONDS)
      )
      _ <- OptionT.liftF(processingTaskDao.updateOwnerAndLease(updated))
    } yield updated).value
      .transact(tx)
  }

  override def extendLease(processingTaskId: ProcessingTaskId, workerNumber: Int, leaseTimeSeconds: Int): Task[Unit] = {
    (for {
      task <- fetchTaskForUpdate(processingTaskId, workerNumber)
      updated = task.copy(nextAction = Instant.now().plus(leaseTimeSeconds.toLong, ChronoUnit.SECONDS))
      _ <- processingTaskDao.updateLease(updated)
    } yield ()).transact(tx)
  }

  override def updateData(
      processingTaskId: ProcessingTaskId,
      workerNumber: Int,
      data: ProcessingTaskData
  ): Task[Unit] = {
    (for {
      task <- fetchTaskForUpdate(processingTaskId, workerNumber)
      updated = task.copy(data = data)
      _ <- processingTaskDao.update(updated)
    } yield ()).transact(tx)
  }

  override def scheduleTask(
      processingTaskId: ProcessingTaskId,
      workerNumber: Int,
      state: S,
      data: ProcessingTaskData,
      scheduledTime: Instant
  ): Task[Unit] = {
    (for {
      task <- fetchTaskForUpdate(processingTaskId, workerNumber)
      updated = task.copy(
        owner = None,
        state = state,
        data = data,
        nextAction = scheduledTime
      )
      _ <- processingTaskDao.update(updated)
    } yield ()).transact(tx)
  }

  override def updateTaskAndExtendLease(
      processingTaskId: ProcessingTaskId,
      workerNumber: Int,
      state: S,
      data: ProcessingTaskData,
      leaseTimeSeconds: Int
  ): Task[ProcessingTask[S]] = {
    (for {
      task <- fetchTaskForUpdate(processingTaskId, workerNumber)
      updated = task.copy(
        state = state,
        data = data,
        nextAction = Instant.now().plus(leaseTimeSeconds.toLong, ChronoUnit.SECONDS)
      )
      _ <- processingTaskDao.update(updated)
    } yield updated).transact(tx)
  }

  override def deleteTask(processingTaskId: ProcessingTaskId, workerNumber: Int): Task[Unit] = {
    processingTaskDao
      .findById(processingTaskId)
      .flatMap {
        case Some(processingTask: ProcessingTask[S])
            if processingTask.owner.nonEmpty && !processingTask.owner.contains(workerId(workerNumber)) =>
          FC.raiseError[Unit](
            new RuntimeException(
              s"Processing task with id: $processingTaskId is currently owned by ${processingTask.owner}. " +
                "Processing task can only be deleted when current worker owns it or it's not owned by any instance."
            )
          )
        case Some(_) => processingTaskDao.delete(processingTaskId).void
        case None => FC.pure(())
      }
      .transact(tx)
  }

  private def fetchTaskForUpdate(
      processingTaskId: ProcessingTaskId,
      workerNumber: Int
  ): ConnectionIO[ProcessingTask[S]] = {
    processingTaskDao.findById(processingTaskId).flatMap {
      case Some(processingTask: ProcessingTask[S]) if !processingTask.owner.contains(workerId(workerNumber)) =>
        FC.raiseError(
          new RuntimeException(
            s"Processing task with id: $processingTaskId has been ejected and it's currently owned by ${processingTask.owner}"
          )
        )
      case Some(processingTask: ProcessingTask[S]) => FC.pure(processingTask)
      case None => FC.raiseError(new RuntimeException(s"Processing task with id: $processingTaskId doesn't exist"))
    }

  }
}
