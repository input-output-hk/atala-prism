package io.iohk.atala.prism.kycbridge.message.processors

import java.time.Instant

import scala.util.Try

import io.circe.syntax._

import io.iohk.atala.prism.kycbridge.task.lease.system.KycBridgeProcessingTaskState
import io.iohk.atala.prism.kycbridge.task.lease.system.data.SendForAcuantManualReviewStateData
import io.iohk.atala.prism.protos.connector_models.ReceivedMessage
import io.iohk.atala.prism.protos.credential_models.{SendForAcuantManualReview, AtalaMessage}
import io.iohk.atala.prism.services.MessageProcessor
import io.iohk.atala.prism.task.lease.system.{ProcessingTaskData, ProcessingTaskService}
import io.iohk.atala.prism.utils.Base64ByteArrayWrapper
import io.iohk.atala.prism.protos.credential_models.{KycBridgeMessage, SentForAcuantManualReview}

class SendForAcuantManualReviewMessageProcessor(
    processingTaskService: ProcessingTaskService[KycBridgeProcessingTaskState]
) {

  val processor: MessageProcessor = { receivedMessage =>
    parseSendForAcuantManualReviewMessage(receivedMessage)
      .map { message =>
        val sendForAcuantManualReviewStateData = SendForAcuantManualReviewStateData(
          receivedMessageId = receivedMessage.id,
          connectionId = receivedMessage.connectionId,
          documentInstanceId = message.documentInstanceId,
          selfieImage = Base64ByteArrayWrapper(message.selfieImage.toByteArray)
        )

        val processingTaskData = ProcessingTaskData(sendForAcuantManualReviewStateData.asJson)

        val responseMessage = AtalaMessage().withKycBridgeMessage(
          KycBridgeMessage().withSentForAcuantManualReview(SentForAcuantManualReview())
        )

        processingTaskService
          .create(processingTaskData, KycBridgeProcessingTaskState.SendForAcuantManualReviewState, Instant.now())
          .flatMap(_ => MessageProcessor.successful(Some(responseMessage)))
      }
  }

  private[processors] def parseSendForAcuantManualReviewMessage(
      message: ReceivedMessage
  ): Option[SendForAcuantManualReview] = {
    Try(AtalaMessage.parseFrom(message.message.toByteArray)).toOption
      .flatMap(_.message.kycBridgeMessage)
      .flatMap(_.message.sendForAcuantManualReview)
  }

}
