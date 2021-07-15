package io.iohk.atala.prism.kycbridge.task.lease.system.processors

import io.circe.syntax._
import monix.eval.Task
import org.scalatest.OptionValues

import io.iohk.atala.prism.kycbridge.KycBridgeFixtures
import io.iohk.atala.prism.kycbridge.models.acas.AccessTokenResponseBody
import io.iohk.atala.prism.kycbridge.models.assureId.NewDocumentInstanceResponseBody
import io.iohk.atala.prism.kycbridge.stubs.{AcasServiceStub, AssureIdServiceStub}
import io.iohk.atala.prism.kycbridge.task.lease.system.KycBridgeProcessingTaskState
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.atala.prism.stubs.ConnectorClientServiceStub
import io.iohk.atala.prism.task.lease.system.{ProcessingTaskData, ProcessingTaskFixtures, ProcessingTaskResult}
import io.iohk.atala.prism.kycbridge.stubs.IdentityMindServiceStub
import io.iohk.atala.prism.kycbridge.models.identityMind.PostConsumerResponse
import io.iohk.atala.prism.kycbridge.task.lease.system.data.SendForAcuantManualReviewStateData
import io.iohk.atala.prism.utils.Base64ByteArrayWrapper

import monix.execution.Scheduler.Implicits.global
import io.iohk.atala.prism.kycbridge.config.IdentityMindConfig

class SendForAcuantManualReviewStateProcessorSpec
    extends PostgresRepositorySpec[Task]
    with KycBridgeFixtures
    with OptionValues {
  import ConnectionFixtures._

  "SendForAcuantManualReviewStateProcessor" should {
    "delay task when assure id service is not available" in new Fixtures {
      processor
        .process(processingTaskWithConnectionData, workerNumber)
        .runSyncUnsafe() mustBe an[ProcessingTaskResult.ProcessingTaskScheduled[
        KycBridgeProcessingTaskState.SendForAcuantManualReviewPendingState.type
      ]]
    }
  }

  trait Fixtures extends ProcessingTaskFixtures {
    val connectorClientStub = new ConnectorClientServiceStub()
    val accessTokenResponseBody = AccessTokenResponseBody(
      accessToken = "accessToken",
      tokenType = "tokenType",
      expiresIn = 0
    )
    val acasServiceStub = new AcasServiceStub(Right(accessTokenResponseBody))

    val newDocumentInstanceResponseBody = NewDocumentInstanceResponseBody(
      documentId = "documentId"
    )
    val assureIdServiceStub = new AssureIdServiceStub(Right(newDocumentInstanceResponseBody))
    val identityMindServiceStub = new IdentityMindServiceStub(
      postConsumerResponse = Right(
        PostConsumerResponse(
          mtid = "mtid",
          user = "test",
          upr = None,
          frn = None,
          frp = None,
          frd = None,
          arpr = None
        )
      )
    )

    val identityMindConfig = IdentityMindConfig(
      url = "https://sandbox.identitymind.com",
      username = "",
      password = "",
      profile = "assureid"
    )

    val processor = new SendForAcuantManualReviewStateProcessor(
      assureIdServiceStub,
      identityMindServiceStub,
      identityMindConfig
    )

    ConnectionFixtures.insertAll(database).runSyncUnsafe()

    val processingTaskWithConnectionData = createProcessingTask[KycBridgeProcessingTaskState](
      state = KycBridgeProcessingTaskState.SendForAcuantManualReviewState,
      data = ProcessingTaskData(
        SendForAcuantManualReviewStateData(
          receivedMessageId = "id",
          connectionId = connection1.id.map(_.uuid.toString).getOrElse(""),
          documentInstanceId = "id",
          selfieImage = Base64ByteArrayWrapper(Array.empty)
        ).asJson
      )
    )
  }

}
