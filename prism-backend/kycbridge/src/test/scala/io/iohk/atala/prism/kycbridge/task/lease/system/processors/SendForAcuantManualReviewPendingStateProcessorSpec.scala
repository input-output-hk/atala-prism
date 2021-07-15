package io.iohk.atala.prism.kycbridge.task.lease.system.processors

import io.circe.syntax._
import monix.eval.Task
import org.scalatest.OptionValues

import io.iohk.atala.prism.kycbridge.KycBridgeFixtures
import io.iohk.atala.prism.kycbridge.task.lease.system.KycBridgeProcessingTaskState
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.atala.prism.task.lease.system.{ProcessingTaskData, ProcessingTaskFixtures, ProcessingTaskResult}
import io.iohk.atala.prism.kycbridge.stubs.IdentityMindServiceStub
import io.iohk.atala.prism.kycbridge.task.lease.system.data.SendForAcuantManualReviewPendingStateData
import io.iohk.atala.prism.utils.Base64ByteArrayWrapper
import io.iohk.atala.prism.stubs.ConnectorClientServiceStub

import monix.execution.Scheduler.Implicits.global
import io.iohk.atala.prism.kycbridge.models.identityMind.{GetConsumerResponse, EdnaScoreCard, ConsumerResponseState}

class SendForAcuantManualReviewPendingStateProcessorSpec
    extends PostgresRepositorySpec[Task]
    with KycBridgeFixtures
    with OptionValues {
  import ConnectionFixtures._

  "SendForAcuantManualReviewPendingStateProcessor" should {
    "reply task when not ready" in new Fixtures {
      override val identityMindServiceStub = new IdentityMindServiceStub(
        getConsumerResponse = Right(
          GetConsumerResponse(
            mtid = "mtid",
            state = ConsumerResponseState.Review,
            ednaScoreCard = EdnaScoreCard(etr = Nil)
          )
        )
      )

      override val procesor = new SendForAcuantManualReviewPendingStateProcessor(
        connectorClientStub,
        identityMindServiceStub
      )

      procesor
        .process(processingData, workerNumber)
        .runSyncUnsafe() mustBe an[ProcessingTaskResult.ProcessingTaskScheduled[
        KycBridgeProcessingTaskState.SendForAcuantManualReviewPendingState.type
      ]]
    }

    "return error when stat is deny/reject" in new Fixtures {
      override val identityMindServiceStub = new IdentityMindServiceStub(
        getConsumerResponse = Right(
          GetConsumerResponse(
            mtid = "mtid",
            state = ConsumerResponseState.Deny,
            ednaScoreCard = EdnaScoreCard(etr = Nil)
          )
        )
      )

      override val procesor = new SendForAcuantManualReviewPendingStateProcessor(
        connectorClientStub,
        identityMindServiceStub
      )

      procesor
        .process(processingData, workerNumber)
        .runSyncUnsafe() mustBe ProcessingTaskResult.ProcessingTaskFinished
    }

    "move to ready state on success" in new Fixtures {
      procesor
        .process(processingData, workerNumber)
        .runSyncUnsafe() mustBe an[ProcessingTaskResult.ProcessingTaskStateTransition[
        KycBridgeProcessingTaskState.SendForAcuantManualReviewReadyState.type
      ]]
    }
  }

  trait Fixtures extends ProcessingTaskFixtures {
    val connectorClientStub = new ConnectorClientServiceStub()
    val identityMindServiceStub = new IdentityMindServiceStub()

    val procesor = new SendForAcuantManualReviewPendingStateProcessor(
      connectorClientStub,
      identityMindServiceStub
    )

    ConnectionFixtures.insertAll(database).runSyncUnsafe()

    val processingData = createProcessingTask[KycBridgeProcessingTaskState](
      state = KycBridgeProcessingTaskState.SendForAcuantManualReviewPendingState,
      data = ProcessingTaskData(
        SendForAcuantManualReviewPendingStateData(
          receivedMessageId = "id",
          connectionId = connection1.id.map(_.uuid.toString).getOrElse(""),
          documentInstanceId = "id",
          selfieImage = Base64ByteArrayWrapper(Array.empty),
          mtid = "mtid"
        ).asJson
      )
    )
  }

}
