package io.iohk.atala.prism.task.lease.system

import io.circe.Json
import io.iohk.atala.prism.stubs.ProcessingTaskServiceStub
import monix.eval.Task
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import monix.execution.Scheduler.Implicits.global

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeoutException
import scala.concurrent.duration._

class ProcessingTaskSchedulerSpec extends AnyWordSpec with Matchers {

  "ProcessingTaskScheduler" should {
    "process task with ProcessingTaskFinished result" in new Fixtures {
      val processingTaskRouter = new ProcessingTaskRouter {
        override def process(processingTask: ProcessingTask): Task[ProcessingTaskResult] =
          Task.pure(ProcessingTaskResult.ProcessingTaskFinished)
      }

      val processingTaskScheduler =
        new ProcessingTaskScheduler(processingTaskServiceStub, processingTaskRouter, taskLeaseConfig)

      val result = processingTaskScheduler.process(processingTask, 1).attempt.runSyncUnsafe()
      result mustBe an[Right[_, _]]

      processingTaskServiceStub.deleteInvokeCount.get() mustBe 1
    }

    "process task with ProcessingTaskScheduled result" in new Fixtures {
      val processingTaskRouter = new ProcessingTaskRouter {
        override def process(processingTask: ProcessingTask): Task[ProcessingTaskResult] =
          Task.pure(ProcessingTaskResult.ProcessingTaskScheduled(ProcessingTaskState.TestState1, taskData, nextAction))
      }

      val processingTaskScheduler =
        new ProcessingTaskScheduler(processingTaskServiceStub, processingTaskRouter, taskLeaseConfig)

      val result = processingTaskScheduler.process(processingTask, 1).attempt.runSyncUnsafe()
      result mustBe an[Right[_, _]]

      processingTaskServiceStub.scheduleTaskInvokeCount.get() mustBe 1
    }

    "process task with ProcessingTaskStateTransition result" in new Fixtures {
      val processingTaskRouter = new ProcessingTaskRouter {
        var count = 0
        override def process(processingTask: ProcessingTask): Task[ProcessingTaskResult] =
          if (count < 3) {
            count = count + 1
            Task.pure(ProcessingTaskResult.ProcessingTaskStateTransition(taskState, taskData))
          } else Task.pure(ProcessingTaskResult.ProcessingTaskFinished)
      }

      val processingTaskScheduler =
        new ProcessingTaskScheduler(processingTaskServiceStub, processingTaskRouter, taskLeaseConfig)

      val result = processingTaskScheduler.process(processingTask, 1).attempt.runSyncUnsafe()
      result mustBe an[Right[_, _]]

      processingTaskServiceStub.updateTaskAndExtendLeaseInvokeCount.get() mustBe 3
      processingTaskServiceStub.deleteInvokeCount.get() mustBe 1
    }

    "extend lease in specified periods of time" in new Fixtures {
      val processingTaskRouter = new ProcessingTaskRouter {
        override def process(processingTask: ProcessingTask): Task[ProcessingTaskResult] =
          Task.pure(ProcessingTaskResult.ProcessingTaskFinished).delayExecution(2500.milliseconds)
      }

      val processingTaskScheduler =
        new ProcessingTaskScheduler(processingTaskServiceStub, processingTaskRouter, taskLeaseConfig)

      val result = processingTaskScheduler.process(processingTask, 1).attempt.runSyncUnsafe()
      result mustBe an[Right[_, _]]

      processingTaskServiceStub.extendLeaseInvokeCount.get() mustBe 2
      processingTaskServiceStub.deleteInvokeCount.get() mustBe 1
    }

    "do not extend lease when processing of tasks fails" in new Fixtures {
      val processingTaskRouter = new ProcessingTaskRouter {
        override def process(processingTask: ProcessingTask): Task[ProcessingTaskResult] =
          Task.raiseError(new Exception("Expected error"))
      }

      val processingTaskScheduler =
        new ProcessingTaskScheduler(processingTaskServiceStub, processingTaskRouter, taskLeaseConfig)

      val result = processingTaskScheduler.process(processingTask, 1).attempt.runSyncUnsafe()
      result mustBe an[Right[_, _]]

      processingTaskServiceStub.extendLeaseInvokeCount.get() mustBe 0
      processingTaskServiceStub.deleteInvokeCount.get() mustBe 0
      processingTaskServiceStub.updateTaskAndExtendLeaseInvokeCount.get() mustBe 0
      processingTaskServiceStub.scheduleTaskInvokeCount.get() mustBe 0
    }

    "process tasks" in new Fixtures {
      val processingTaskRouter = new ProcessingTaskRouter {
        override def process(processingTask: ProcessingTask): Task[ProcessingTaskResult] =
          Task.pure(ProcessingTaskResult.ProcessingTaskFinished)
      }

      override val processingTaskServiceStub = new ProcessingTaskServiceStub() {

        @volatile
        var count = 0

        override def fetchTaskToProcess(leaseTimeSeconds: Int): Task[Option[ProcessingTask]] = {
          count = count + 1
          if (count != 3) Task.pure(None)
          else Task.pure(Some(processingTask))
        }
      }

      val processingTaskScheduler =
        new ProcessingTaskScheduler(processingTaskServiceStub, processingTaskRouter, taskLeaseConfig)

      intercept[TimeoutException] {
        processingTaskScheduler.run.runSyncUnsafe(timeout = 3.seconds)
      }

      processingTaskServiceStub.deleteInvokeCount.get() mustBe 1
    }

    "notify idle worker when there is a new task to be processed" in new Fixtures {
      val processingTaskRouter = new ProcessingTaskRouter {
        override def process(processingTask: ProcessingTask): Task[ProcessingTaskResult] =
          Task.pure(ProcessingTaskResult.ProcessingTaskFinished)
      }

      override val processingTaskServiceStub = new ProcessingTaskServiceStub() {

        @volatile
        var count = 0

        override def fetchTaskToProcess(leaseTimeSeconds: Int): Task[Option[ProcessingTask]] = {
          count = count + 1
          if (count != 2) Task.pure(None)
          else Task.pure(Some(processingTask))
        }
      }

      val newTaskLeaseConfig = taskLeaseConfig.copy(workerSleepTimeSeconds = 10)

      val processingTaskScheduler =
        new ProcessingTaskScheduler(processingTaskServiceStub, processingTaskRouter, newTaskLeaseConfig)

      intercept[TimeoutException] {
        Task
          .parSequence(
            Seq(
              processingTaskScheduler.run,
              Task.sleep(100.milliseconds).map(_ => processingTaskScheduler.notifyIdleWorker())
            )
          )
          .runSyncUnsafe(timeout = 200.milliseconds)
      }

      processingTaskServiceStub.deleteInvokeCount.get() mustBe 1
    }

  }

  trait Fixtures {
    val taskData = ProcessingTaskData(Json.fromString("sample"))
    val taskState = ProcessingTaskState.TestState1
    val processingTaskServiceStub = new ProcessingTaskServiceStub()
    val nextAction = Instant.now().plus(30, ChronoUnit.SECONDS)

    val taskLeaseConfig = new ProcessingTaskLeaseConfig(
      leaseTimeSeconds = 30,
      extendLeaseTimeIntervalSeconds = 1,
      numberOfWorkers = 1,
      workerSleepTimeSeconds = 1
    )

    val processingTask = ProcessingTask(
      id = ProcessingTaskId.random(),
      state = taskState,
      owner = None,
      lastChange = Instant.now(),
      nextAction = nextAction,
      data = taskData
    )
  }

}
