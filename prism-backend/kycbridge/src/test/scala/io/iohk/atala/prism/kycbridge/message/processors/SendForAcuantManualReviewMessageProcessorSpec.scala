package io.iohk.atala.prism.kycbridge.message.processors

import cats.implicits.catsSyntaxOptionId
import io.iohk.atala.prism.kycbridge.KycBridgeFixtures
import io.iohk.atala.prism.kycbridge.task.lease.system.KycBridgeProcessingTaskState
import io.iohk.atala.prism.protos.connector_models.ReceivedMessage
import io.iohk.atala.prism.protos.credential_models.{SendForAcuantManualReview, AtalaMessage, KycBridgeMessage}
import io.iohk.atala.prism.services.ServicesFixtures
import io.iohk.atala.prism.stubs.ProcessingTaskServiceStub
import io.iohk.atala.prism.utils.syntax.InstantToTimestampOps
import monix.execution.Scheduler.Implicits.global
import org.scalatest.OptionValues._
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.{LocalDateTime, ZoneOffset}

class SendForAcuantManualReviewMessageProcessorSpec
    extends AnyWordSpec
    with Matchers
    with ServicesFixtures
    with KycBridgeFixtures {
  import ConnectionFixtures._

  "SendForAcuantManualReviewMessageProcessor" should {
    "parse SendForAcuantManualReview message" in new Fixtures {
      processor.parseSendForAcuantManualReviewMessage(receivedMessage) mustBe Some(sendForAcuantManualReview)
    }

    "process message and create a new processing task" in new Fixtures {
      processor.processor(receivedMessage).value.runSyncUnsafe() mustBe Right(None)
      processingTaskServiceStub.createTaskInvokeCount.get() mustBe 1
    }
  }

  trait Fixtures {
    val processingTaskServiceStub = new ProcessingTaskServiceStub[KycBridgeProcessingTaskState]()
    val processor = new SendForAcuantManualReviewMessageProcessor(processingTaskServiceStub)

    val sendForAcuantManualReview = SendForAcuantManualReview(
      documentInstanceId = "id"
    )

    val receivedMessage = ReceivedMessage(
      id = "id1",
      received = LocalDateTime.of(2020, 6, 12, 0, 0).toInstant(ZoneOffset.UTC).toProtoTimestamp.some,
      connectionId = connection1.id.get.uuid.toString,
      message = AtalaMessage()
        .withKycBridgeMessage(KycBridgeMessage().withSendForAcuantManualReview(sendForAcuantManualReview))
        .toByteString
    )
  }
}
