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

trait ProcessingTaskService {

  /**
    * Registers a callback which will be called when a new processing task is created and it should be run
    * immediately (scheduledTime <= Instant.now()).
    */
  def registerNotifyIdleWorkerCallback(callback: () => Unit): Unit

  /**
    * Creates a completely new processing task that will be processed after scheduled time.
    */
  def create(
      processingTaskData: ProcessingTaskData,
      processingTaskState: ProcessingTaskState,
      scheduledTime: Instant
  ): Task[ProcessingTaskId]

  /**
    * Acquires a processing task for a specified period of time. Returns None when there is no task to process.
    */
  def fetchTaskToProcess(leaseTimeSeconds: Int): Task[Option[ProcessingTask]]

  /**
    * Acquires tha task specified by id immediately, even though it's being processed by another instance which has
    * a valid lease. Returns None when task doesn't exist.
    */
  def ejectTask(processingTaskId: ProcessingTaskId, leaseTimeSeconds: Int): Task[Option[ProcessingTask]]

  /**
    * Extends a lese of the processing task by specified number of seconds.
    * Fails when the processing task has been ejected by another instance.
    */
  def extendLease(processingTaskId: ProcessingTaskId, leaseTimeSeconds: Int): Task[Unit]

  /**
    * Updates the processing task data.
    * Fails when the processing task has been ejected by another instance.
    */
  def updateData(
      processingTaskId: ProcessingTaskId,
      data: ProcessingTaskData
  ): Task[Unit]

  /**
    * Schedules the processing task to be run after scheduled time at a new state and with new data.
    * Clears the owner of the task.
    * Fails when the processing task has been ejected by another instance.
    */
  def scheduleTask(
      processingTaskId: ProcessingTaskId,
      state: ProcessingTaskState,
      data: ProcessingTaskData,
      scheduledTime: Instant
  ): Task[Unit]

  /**
    * Extends a lease of the processing and updates the processing task with a new state and data.
    * Fails when the processing task has been ejected by another instance.
    */
  def updateTaskAndExtendLease(
      processingTaskId: ProcessingTaskId,
      state: ProcessingTaskState,
      data: ProcessingTaskData,
      leaseTimeSeconds: Int
  ): Task[ProcessingTask]

  /**
    * Deletes the processing task completely from database. Processing task can only be deleted when
    *  current instance own it or it's not owned by any instance.
    * Fails when the processing task has been ejected by another instance.
    */
  def deleteTask(processingTaskId: ProcessingTaskId): Task[Unit]
}

class ProcessingTaskServiceImpl(tx: Transactor[Task], currentInstanceUUID: UUID) extends ProcessingTaskService {

  var callbackOption: Option[() => Unit] = None

  def registerNotifyIdleWorkerCallback(callback: () => Unit): Unit = {
    callbackOption = Some(callback)
  }

  override def create(
      processingTaskData: ProcessingTaskData,
      processingTaskState: ProcessingTaskState,
      scheduledTime: Instant
  ): Task[ProcessingTaskId] = {
    val processingTask = ProcessingTask(
      id = ProcessingTaskId.random(),
      state = processingTaskState,
      owner = None,
      lastChange = Instant.now(),
      nextAction = scheduledTime,
      data = processingTaskData
    )

    ProcessingTaskDao.insert(processingTask).transact(tx).map(_ => processingTask.id).map { id =>
      if (!scheduledTime.isAfter(Instant.now())) {
        callbackOption.foreach(callback => callback())
      }
      id
    }
  }

  override def fetchTaskToProcess(leaseTimeSeconds: Int): Task[Option[ProcessingTask]] = {
    (for {
      taskToProcess <- OptionT(ProcessingTaskDao.fetchTaskToProcess())
      updated = taskToProcess.copy(
        owner = Some(currentInstanceUUID),
        nextAction = Instant.now().plus(leaseTimeSeconds.toLong, ChronoUnit.SECONDS)
      )
      _ <- OptionT.liftF(ProcessingTaskDao.updateOwnerAndLease(updated))
    } yield updated).value
      .transact(tx)
  }

  override def ejectTask(processingTaskId: ProcessingTaskId, leaseTimeSeconds: Int): Task[Option[ProcessingTask]] = {
    (for {
      task <- OptionT(ProcessingTaskDao.findById(processingTaskId))
      updated = task.copy(
        owner = Some(currentInstanceUUID),
        nextAction = Instant.now().plus(leaseTimeSeconds.toLong, ChronoUnit.SECONDS)
      )
      _ <- OptionT.liftF(ProcessingTaskDao.updateOwnerAndLease(updated))
    } yield updated).value
      .transact(tx)
  }

  override def extendLease(processingTaskId: ProcessingTaskId, leaseTimeSeconds: Int): Task[Unit] = {
    (for {
      task <- fetchTaskForUpdate(processingTaskId)
      updated = task.copy(nextAction = Instant.now().plus(leaseTimeSeconds.toLong, ChronoUnit.SECONDS))
      _ <- ProcessingTaskDao.updateLease(updated)
    } yield ()).transact(tx)
  }

  override def updateData(
      processingTaskId: ProcessingTaskId,
      data: ProcessingTaskData
  ): Task[Unit] = {
    (for {
      task <- fetchTaskForUpdate(processingTaskId)
      updated = task.copy(data = data)
      _ <- ProcessingTaskDao.update(updated)
    } yield ()).transact(tx)
  }

  override def scheduleTask(
      processingTaskId: ProcessingTaskId,
      state: ProcessingTaskState,
      data: ProcessingTaskData,
      scheduledTime: Instant
  ): Task[Unit] = {
    (for {
      task <- fetchTaskForUpdate(processingTaskId)
      updated = task.copy(
        owner = None,
        state = state,
        data = data,
        nextAction = scheduledTime
      )
      _ <- ProcessingTaskDao.update(updated)
    } yield ()).transact(tx)
  }

  override def updateTaskAndExtendLease(
      processingTaskId: ProcessingTaskId,
      state: ProcessingTaskState,
      data: ProcessingTaskData,
      leaseTimeSeconds: Int
  ): Task[ProcessingTask] = {
    (for {
      task <- fetchTaskForUpdate(processingTaskId)
      updated = task.copy(
        state = state,
        data = data,
        nextAction = Instant.now().plus(leaseTimeSeconds.toLong, ChronoUnit.SECONDS)
      )
      _ <- ProcessingTaskDao.update(updated)
    } yield updated).transact(tx)
  }

  override def deleteTask(processingTaskId: ProcessingTaskId): Task[Unit] = {
    ProcessingTaskDao
      .findById(processingTaskId)
      .flatMap {
        case Some(processingTask: ProcessingTask)
            if processingTask.owner.nonEmpty && !processingTask.owner.contains(currentInstanceUUID) =>
          FC.raiseError[Unit](
            new RuntimeException(
              s"Processing task with id: $processingTaskId is currently owned by instance ${processingTask.owner}. " +
                "Processing task can only be deleted when current instance own it or it's not owned by any instance."
            )
          )
        case Some(_) => ProcessingTaskDao.delete(processingTaskId).void
        case None => FC.pure(())
      }
      .transact(tx)
  }

  private def fetchTaskForUpdate(processingTaskId: ProcessingTaskId): ConnectionIO[ProcessingTask] = {
    ProcessingTaskDao.findById(processingTaskId).flatMap {
      case Some(processingTask: ProcessingTask) if !processingTask.owner.contains(currentInstanceUUID) =>
        FC.raiseError(
          new RuntimeException(
            s"Processing task with id: $processingTaskId has been ejected and it's currently owned by instance ${processingTask.owner}"
          )
        )
      case Some(processingTask: ProcessingTask) => FC.pure(processingTask)
      case None => FC.raiseError(new RuntimeException(s"Processing task with id: $processingTaskId doesn't exist"))
    }

  }
}
