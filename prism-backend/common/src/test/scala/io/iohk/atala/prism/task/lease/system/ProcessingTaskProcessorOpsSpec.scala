package io.iohk.atala.prism.task.lease.system

import io.circe.{Json, _}
import io.circe.generic.semiauto._
import io.circe.syntax._
import io.iohk.atala.prism.auth.errors.InvalidAtalaOperationError
import io.iohk.atala.prism.stubs.ConnectorClientServiceStub
import io.iohk.atala.prism.task.lease.system.ProcessingTaskProcessorOps._
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.slf4j.LoggerFactory

class ProcessingTaskProcessorOpsSpec extends AnyWordSpec with Matchers {

  "ProcessingTaskProcessorOps" should {
    "sendResponseOnError" should {
      "not send error when result is Right" in new Fixtures {
        Task
          .pure(Right(()))
          .sendResponseOnError(connectorClient, receivedMessageId, connectionId)
          .runSyncUnsafe() mustBe Right(())
        connectorClient.sendMessageInvokeCount.get() mustBe 0
      }

      "send error when result is Left" in new Fixtures {
        Task
          .pure(Left(InvalidAtalaOperationError))
          .sendResponseOnError(connectorClient, receivedMessageId, connectionId)
          .runSyncUnsafe() mustBe Left(InvalidAtalaOperationError)
        connectorClient.sendMessageInvokeCount.get() mustBe 1
      }
    }

    "mapErrorToProcessingTaskFinished" should {
      "do not map error when result is Right" in new Fixtures {
        Task
          .pure(Right(()))
          .mapErrorToProcessingTaskFinished()
          .runSyncUnsafe() mustBe Right(())
      }

      "map error when result is Left" in new Fixtures {
        Task
          .pure(Left(InvalidAtalaOperationError))
          .mapErrorToProcessingTaskFinished()
          .runSyncUnsafe() mustBe Left(ProcessingTaskResult.ProcessingTaskFinished)
      }
    }

    "mapErrorToProcessingTaskScheduled" should {
      "do not map error when result is Right" in new Fixtures {
        Task
          .pure(Right(()))
          .mapErrorToProcessingTaskScheduled(processingTask)
          .runSyncUnsafe() mustBe Right(())
      }

      "map error when result is Left" in new Fixtures {
        Task
          .pure(Left(InvalidAtalaOperationError))
          .mapErrorToProcessingTaskScheduled(processingTask)
          .runSyncUnsafe() mustBe an[Left[ProcessingTaskResult.ProcessingTaskScheduled[ProcessingTaskTestState], Unit]]
      }
    }

    "parseProcessingTaskData" should {
      implicit val logger = LoggerFactory.getLogger(this.getClass)
      "parse data when data is ok" in new Fixtures {
        parseProcessingTaskData[TestData, ProcessingTaskTestState](processingTask).value
          .runSyncUnsafe() mustBe Right(testData)
      }

      "return ProcessingTaskFinished when data is incorrect" in new Fixtures {
        parseProcessingTaskData[TestData, ProcessingTaskTestState](
          processingTask.copy(data = ProcessingTaskData(Json.obj()))
        ).value
          .runSyncUnsafe() mustBe Left(ProcessingTaskResult.ProcessingTaskFinished)
      }
    }
  }

  trait Fixtures extends ProcessingTaskFixtures {
    val connectorClient = new ConnectorClientServiceStub()
    val receivedMessageId = "receivedMessageId"
    val connectionId = "connectionId"

    case class TestData(value: String)
    val testData = TestData("test")

    implicit val testDataEncoder: Encoder[TestData] = deriveEncoder
    implicit val testDataDecoder: Decoder[TestData] = deriveDecoder

    val processingTask = createProcessingTask[ProcessingTaskTestState](
      state = ProcessingTaskTestState.TestState1,
      data = ProcessingTaskData(testData.asJson)
    )
  }

}
