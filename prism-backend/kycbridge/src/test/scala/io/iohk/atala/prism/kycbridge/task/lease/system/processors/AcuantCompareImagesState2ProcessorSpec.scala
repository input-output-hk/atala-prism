package io.iohk.atala.prism.kycbridge.task.lease.system.processors

import io.circe.syntax._
import io.iohk.atala.prism.kycbridge.KycBridgeFixtures
import io.iohk.atala.prism.kycbridge.models.assureId.Document
import io.iohk.atala.prism.kycbridge.models.faceId.FaceMatchResponse
import io.iohk.atala.prism.kycbridge.services.AssureIdService.AssureIdServiceError
import io.iohk.atala.prism.kycbridge.services.FaceIdService.FaceIdServiceError
import io.iohk.atala.prism.kycbridge.stubs.{AssureIdServiceStub, FaceIdServiceStub}
import io.iohk.atala.prism.kycbridge.task.lease.system.KycBridgeProcessingTaskState
import io.iohk.atala.prism.kycbridge.task.lease.system.data.AcuantCompareImagesState2Data
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.atala.prism.stubs.ConnectorClientServiceStub
import io.iohk.atala.prism.task.lease.system.ProcessingTaskResult.{
  ProcessingTaskFinished,
  ProcessingTaskScheduled,
  ProcessingTaskStateTransition
}
import io.iohk.atala.prism.task.lease.system.{ProcessingTaskData, ProcessingTaskFixtures}
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global

class AcuantCompareImagesState2ProcessorSpec extends PostgresRepositorySpec[Task] with KycBridgeFixtures {
  import ConnectionFixtures._

  "AcuantCompareImagesState2Processor" should {
    "transition to next state when face match succeeded" in new Fixtures {
      val result = processor.process(processingTask).runSyncUnsafe()
      result mustBe an[ProcessingTaskStateTransition[KycBridgeProcessingTaskState]]
    }

    "send error message and finish process when face match failed" in new Fixtures {
      override val faceIdServiceStub = new FaceIdServiceStub(Right(FaceMatchResponse(score = 0, isMatch = false)))
      override val processor =
        new AcuantCompareImagesState2Processor(connectorClientStub, assureIdServiceStub, faceIdServiceStub)
      val result = processor.process(processingTask).runSyncUnsafe()
      result mustBe ProcessingTaskFinished
      connectorClientStub.sendMessageInvokeCount.get() mustBe 1
    }

    "delay task when face match cannot be performed" in new Fixtures {
      override val faceIdServiceStub = new FaceIdServiceStub(Left(FaceIdServiceError("faceMatch", new Throwable)))
      override val processor =
        new AcuantCompareImagesState2Processor(connectorClientStub, assureIdServiceStub, faceIdServiceStub)
      val result = processor.process(processingTask).runSyncUnsafe()
      result mustBe an[ProcessingTaskScheduled[KycBridgeProcessingTaskState]]
    }

    "delay task when image from id document cannot be fetched" in new Fixtures {
      override val assureIdServiceStub = new AssureIdServiceStub(
        getFrontImageFromDocumentResponse = Left(AssureIdServiceError("getFrontImageFromDocument", new Throwable))
      )
      override val processor =
        new AcuantCompareImagesState2Processor(connectorClientStub, assureIdServiceStub, faceIdServiceStub)
      val result = processor.process(processingTask).runSyncUnsafe()
      result mustBe an[ProcessingTaskScheduled[KycBridgeProcessingTaskState]]
    }
  }

  trait Fixtures extends ProcessingTaskFixtures {
    val connectorClientStub = new ConnectorClientServiceStub()
    val assureIdServiceStub = new AssureIdServiceStub()
    val faceIdServiceStub = new FaceIdServiceStub()
    val processor = new AcuantCompareImagesState2Processor(connectorClientStub, assureIdServiceStub, faceIdServiceStub)

    val state2Data = AcuantCompareImagesState2Data(
      receivedMessageId = "receivedMessageId",
      connectionId = connection1.id.get.uuid.toString,
      documentInstanceId = "documentInstanceId",
      selfieImage = Array.empty[Byte],
      document = Document(instanceId = "id", biographic = None, classification = None, dataFields = None)
    )

    val processingTask = createProcessingTask[KycBridgeProcessingTaskState](
      state = KycBridgeProcessingTaskState.AcuantCompareImagesState2,
      data = ProcessingTaskData(state2Data.asJson)
    )
  }
}
