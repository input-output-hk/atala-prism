package io.iohk.atala.prism.kycbridge.task.lease.system.processors

import doobie.implicits._
import io.circe.syntax._
import io.iohk.atala.prism.kycbridge.KycBridgeFixtures
import io.iohk.atala.prism.kycbridge.db.ConnectionDao
import io.iohk.atala.prism.kycbridge.services.AssureIdService.AssureIdServiceError
import io.iohk.atala.prism.kycbridge.stubs.AssureIdServiceStub
import io.iohk.atala.prism.kycbridge.task.lease.system.KycBridgeProcessingTaskState
import io.iohk.atala.prism.kycbridge.task.lease.system.data.{
  AcuantCompareImagesState2Data,
  AcuantFetchDocumentState1Data
}
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.atala.prism.stubs.ConnectorClientServiceStub
import io.iohk.atala.prism.task.lease.system.ProcessingTaskResult.{
  ProcessingTaskFinished,
  ProcessingTaskScheduled,
  ProcessingTaskStateTransition
}
import io.iohk.atala.prism.task.lease.system.{ProcessingTaskData, ProcessingTaskFixtures}
import io.iohk.atala.prism.utils.Base64ByteArrayWrapper
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.scalatest.OptionValues._

class AcuantFetchDocumentState1ProcessorSpec extends PostgresRepositorySpec[Task] with KycBridgeFixtures {
  import ConnectionFixtures._

  "AcuantFetchDocumentState1Processor" should {
    "fetch document and document status" in new Fixtures {
      val result = processor.process(processingTask).runSyncUnsafe()
      result mustBe ProcessingTaskStateTransition(
        KycBridgeProcessingTaskState.AcuantCompareImagesState2,
        ProcessingTaskData(
          AcuantCompareImagesState2Data
            .fromAcuantFetchDocumentState1Data(
              state1Data,
              assureIdServiceStub.getDocument("documentInstanceId").runSyncUnsafe().toOption.value
            )
            .asJson
        )
      )
      ConnectionDao
        .findByConnectionToken(connection1.token)
        .transact(database)
        .runSyncUnsafe()
        .value
        .acuantDocumentStatus
        .isDefined mustBe true
    }

    "delay task when document is not available" in new Fixtures {
      override val assureIdServiceStub =
        new AssureIdServiceStub(document = Left(AssureIdServiceError("getDocument", new Throwable)))
      override val processor =
        new AcuantFetchDocumentState1Processor(database, connectorClientStub, assureIdServiceStub)
      val result = processor.process(processingTask).runSyncUnsafe()
      result mustBe an[ProcessingTaskScheduled[KycBridgeProcessingTaskState]]
    }

    "delay task when document status is not available" in new Fixtures {
      override val assureIdServiceStub =
        new AssureIdServiceStub(documentStatus = Left(AssureIdServiceError("getDocumentStatus", new Throwable)))
      override val processor =
        new AcuantFetchDocumentState1Processor(database, connectorClientStub, assureIdServiceStub)
      val result = processor.process(processingTask).runSyncUnsafe()
      result mustBe an[ProcessingTaskScheduled[KycBridgeProcessingTaskState]]
    }

    "send error message and finish processing when connection id doesn't exist" in new Fixtures {
      val processingTaskWithWrongConnectionId =
        processingTask.copy(data = ProcessingTaskData(state1Data.copy(connectionId = "badId").asJson))
      val result = processor.process(processingTaskWithWrongConnectionId).runSyncUnsafe()
      result mustBe ProcessingTaskFinished
      connectorClientStub.sendMessageInvokeCount.get() mustBe 1
    }
  }

  trait Fixtures extends ProcessingTaskFixtures {
    val connectorClientStub = new ConnectorClientServiceStub()
    val assureIdServiceStub = new AssureIdServiceStub()
    val processor = new AcuantFetchDocumentState1Processor(database, connectorClientStub, assureIdServiceStub)

    val state1Data = AcuantFetchDocumentState1Data(
      receivedMessageId = "receivedMessageId",
      connectionId = connection1.id.get.uuid.toString,
      documentInstanceId = "documentInstanceId",
      selfieImage = Base64ByteArrayWrapper(Array.empty[Byte])
    )

    val processingTask = createProcessingTask[KycBridgeProcessingTaskState](
      state = KycBridgeProcessingTaskState.AcuantFetchDocumentDataState1,
      data = ProcessingTaskData(state1Data.asJson)
    )

    ConnectionFixtures.insertAll(database).runSyncUnsafe()
  }

}
