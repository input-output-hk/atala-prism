package io.iohk.atala.prism.task.lease.system

import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import doobie.implicits._
import io.circe.Json

import java.time.Instant
import java.util.UUID
import org.scalatest.OptionValues._

import java.time.temporal.ChronoUnit

class ProcessingTaskServiceSpec extends PostgresRepositorySpec[Task] {

  override protected def migrationScriptsLocation: String = "common/db/migration"

  "ProcessingTaskService" should {
    "insert a new task" in new Fixtures {
      val fetchedTask = (for {
        id <- processingTaskService.create(taskData, taskState, scheduledTime)
        fetchedTask <- find(id)
      } yield fetchedTask).runSyncUnsafe().value

      fetchedTask.owner mustBe None
      fetchedTask.state mustBe taskState
      fetchedTask.data mustBe taskData
      fetchedTask.nextAction mustBe scheduledTime
    }

    "fetch task to process and update database" in new Fixtures {
      val taskToProcess = processingTaskService.fetchTaskToProcess(leaseTimeSeconds, workerNumber).runSyncUnsafe().value
      taskToProcess.id mustBe task1NoOwner.id
      taskToProcess.owner mustBe Some(ProcessingTaskOwner(s"$instanceAUuid-$workerNumber"))
      taskToProcess.nextAction.isAfter(Instant.now()) mustBe true

      val updatedTask = find(taskToProcess.id).runSyncUnsafe().value
      updatedTask.owner mustBe Some(ProcessingTaskOwner(s"$instanceAUuid-$workerNumber"))
      updatedTask.nextAction.isAfter(Instant.now()) mustBe true
    }

    "fetch tasks with overrun lease" in new Fixtures {
      val task1 = processingTaskService.fetchTaskToProcess(leaseTimeSeconds, workerNumber).runSyncUnsafe().value
      task1.id mustBe task1NoOwner.id

      val ejectedTask = processingTaskService.fetchTaskToProcess(leaseTimeSeconds, workerNumber).runSyncUnsafe().value
      ejectedTask.id mustBe task2OwnerB.id

      val updatedTask = find(ejectedTask.id).runSyncUnsafe().value
      updatedTask.owner mustBe Some(ProcessingTaskOwner(s"$instanceAUuid-$workerNumber"))
      updatedTask.nextAction.isAfter(Instant.now()) mustBe true
    }

    "eject task" in new Fixtures {
      val taskToProcess =
        processingTaskService.ejectTask(task3OwnerB.id, workerNumber, leaseTimeSeconds).runSyncUnsafe().value
      taskToProcess.id mustBe task3OwnerB.id
      taskToProcess.owner mustBe Some(ProcessingTaskOwner(s"$instanceAUuid-$workerNumber"))
      taskToProcess.nextAction.isAfter(Instant.now()) mustBe true

      val updatedTask = find(taskToProcess.id).runSyncUnsafe().value
      updatedTask.owner mustBe Some(ProcessingTaskOwner(s"$instanceAUuid-$workerNumber"))
      updatedTask.nextAction.isAfter(Instant.now()) mustBe true
    }

    "extend lease of a task" in new Fixtures {
      val updatedTask = (for {
        taskToProcessOption <- processingTaskService.fetchTaskToProcess(leaseTimeSeconds, workerNumber)
        taskToProcess = taskToProcessOption.value
        _ <- processingTaskService.extendLease(taskToProcess.id, workerNumber, 10 * 60)
        updatedTaskOption <- find(taskToProcess.id)
      } yield updatedTaskOption.value).runSyncUnsafe()

      updatedTask.nextAction.isAfter(Instant.now().plus(9 * 60, ChronoUnit.SECONDS)) mustBe true
    }

    "update data of a task" in new Fixtures {
      val updatedTask = (for {
        taskToProcessOption <- processingTaskService.fetchTaskToProcess(leaseTimeSeconds, workerNumber)
        taskToProcess = taskToProcessOption.value
        _ <- processingTaskService.updateData(taskToProcess.id, workerNumber, newData)
        updatedTaskOption <- find(taskToProcess.id)
      } yield updatedTaskOption.value).runSyncUnsafe()

      updatedTask.data mustBe newData
    }

    "schedule a task" in new Fixtures {
      val newNextAction = Instant.now().plus(60, ChronoUnit.SECONDS)

      val updatedTask = (for {
        taskToProcessOption <- processingTaskService.fetchTaskToProcess(leaseTimeSeconds, workerNumber)
        taskToProcess = taskToProcessOption.value
        _ <- processingTaskService.scheduleTask(
          taskToProcess.id,
          workerNumber,
          ProcessingTaskTestState.TestState1,
          newData,
          newNextAction
        )
        updatedTaskOption <- find(taskToProcess.id)
      } yield updatedTaskOption.value).runSyncUnsafe()

      updatedTask.owner mustBe None
      updatedTask.data mustBe newData
      updatedTask.nextAction mustBe newNextAction
    }

    "update task and extend lease" in new Fixtures {
      val updatedTask = (for {
        taskToProcessOption <- processingTaskService.fetchTaskToProcess(leaseTimeSeconds, workerNumber)
        taskToProcess = taskToProcessOption.value
        _ <- processingTaskService.updateTaskAndExtendLease(
          taskToProcess.id,
          workerNumber,
          ProcessingTaskTestState.TestState1,
          newData,
          10 * 60
        )
        updatedTaskOption <- find(taskToProcess.id)
      } yield updatedTaskOption.value).runSyncUnsafe()

      updatedTask.owner mustBe Some(ProcessingTaskOwner(s"$instanceAUuid-$workerNumber"))
      updatedTask.data mustBe newData
      updatedTask.nextAction.isAfter(Instant.now().plus(9 * 60, ChronoUnit.SECONDS)) mustBe true
    }

    "delete task" in new Fixtures {
      val updatedTaskOption = (for {
        taskToProcessOption <- processingTaskService.fetchTaskToProcess(leaseTimeSeconds, workerNumber)
        taskToProcess = taskToProcessOption.value
        _ <- processingTaskService.deleteTask(taskToProcess.id, workerNumber)
        updatedTaskOption <- find(taskToProcess.id)
      } yield updatedTaskOption).runSyncUnsafe()

      updatedTaskOption mustBe None
    }

    "do not update task when task has been ejected by another instance" in new Fixtures {
      val taskToProcess = (for {
        taskToProcessOption <- processingTaskService.fetchTaskToProcess(leaseTimeSeconds, workerNumber)
        taskToProcess = taskToProcessOption.value
        _ <- processingTaskServiceInstanceB.ejectTask(taskToProcess.id, leaseTimeSeconds, workerNumber)
      } yield taskToProcess).runSyncUnsafe()

      val result = processingTaskService.updateData(taskToProcess.id, workerNumber, newData).attempt.runSyncUnsafe()
      result mustBe an[Left[Throwable, Unit]]
    }

    "do not update task when task doesn't exist" in new Fixtures {
      val result =
        processingTaskService.updateData(ProcessingTaskId.random(), workerNumber, newData).attempt.runSyncUnsafe()
      result mustBe an[Left[Throwable, Unit]]
    }

    "invoke callback when scheduledTime <= Instant.now() " in new Fixtures {
      var invokeCount = 0
      processingTaskService.registerNotifyIdleWorkerCallback(() => invokeCount = 1)

      processingTaskService.create(taskData, taskState, Instant.now()).runSyncUnsafe()

      invokeCount mustBe 1
    }
  }

  trait Fixtures {
    val instanceAUuid = UUID.randomUUID()
    val instanceBUuid = UUID.randomUUID()
    val workerNumber = 2
    val instanceBWorkerNumber = 3
    val processingTaskDao = new ProcessingTaskDao(ProcessingTaskTestState.withNameOption)
    val processingTaskService = new ProcessingTaskServiceImpl(database, instanceAUuid, processingTaskDao)
    val processingTaskServiceInstanceB = new ProcessingTaskServiceImpl(database, instanceBUuid, processingTaskDao)

    val taskData = ProcessingTaskData(Json.fromString("sample"))
    val newData = ProcessingTaskData(Json.fromInt(42))
    val leaseTimeSeconds = 30
    val taskState = ProcessingTaskTestState.TestState1
    val scheduledTime = Instant.now()

    val (task1NoOwner, task2OwnerB, task3OwnerB, task4NoOwner) = (for {
      task1NoOwner <- create(None, scheduledTime.minus(2, ChronoUnit.MINUTES))
      task2OwnerB <- create(Some(s"instanceBUuid-$instanceBWorkerNumber"), scheduledTime.minus(1, ChronoUnit.MINUTES))
      task3OwnerB <- create(Some(s"instanceBUuid-$instanceBWorkerNumber"), scheduledTime.plus(1, ChronoUnit.MINUTES))
      task4NoOwner <- create(None, scheduledTime.plus(2, ChronoUnit.MINUTES))
    } yield (
      task1NoOwner,
      task2OwnerB,
      task3OwnerB,
      task4NoOwner
    )).runSyncUnsafe()

    def find(processingTaskId: ProcessingTaskId): Task[Option[ProcessingTask[ProcessingTaskTestState]]] = {
      processingTaskDao.findById(processingTaskId).transact(database)
    }

    def create(owner: Option[String], scheduledTime: Instant): Task[ProcessingTask[ProcessingTaskTestState]] = {
      val processingTask = ProcessingTask[ProcessingTaskTestState](
        id = ProcessingTaskId.random(),
        state = taskState,
        owner = owner.map(ProcessingTaskOwner.apply),
        lastChange = Instant.now(),
        nextAction = scheduledTime,
        data = taskData
      )

      processingTaskDao.insert(processingTask).transact(database).map(_ => processingTask)
    }
  }
}
