package io.iohk.atala.prism.kycbridge.message.processors

import io.circe.syntax._
import io.iohk.atala.prism.kycbridge.task.lease.system.KycBridgeProcessingTaskState
import io.iohk.atala.prism.kycbridge.task.lease.system.data.AcuantFetchDocumentState1Data
import io.iohk.atala.prism.protos.connector_models.ReceivedMessage
import io.iohk.atala.prism.protos.credential_models.{AcuantProcessFinished, AtalaMessage}
import io.iohk.atala.prism.services.MessageProcessor
import io.iohk.atala.prism.task.lease.system.{ProcessingTaskData, ProcessingTaskService}
import io.iohk.atala.prism.utils.Base64ByteArrayWrapper

import java.time.Instant
import scala.util.Try

class AcuantDocumentUploadedMessageProcessor(
    processingTaskService: ProcessingTaskService[KycBridgeProcessingTaskState]
) {

  val processor: MessageProcessor = { receivedMessage =>
    parseAcuantProcessFinishedMessage(receivedMessage)
      .map { message =>
        val acuantProcessingData = AcuantFetchDocumentState1Data(
          receivedMessageId = receivedMessage.id,
          connectionId = receivedMessage.connectionId,
          documentInstanceId = message.documentInstanceId,
          selfieImage = Base64ByteArrayWrapper(message.selfieImage.toByteArray)
        )

        val processingTaskData = ProcessingTaskData(acuantProcessingData.asJson)

        processingTaskService
          .create(processingTaskData, KycBridgeProcessingTaskState.AcuantFetchDocumentDataState1, Instant.now())
          .map(_ => Right(None))
      }
  }

  private[processors] def parseAcuantProcessFinishedMessage(
      message: ReceivedMessage
  ): Option[AcuantProcessFinished] = {
    Try(AtalaMessage.parseFrom(message.message.toByteArray)).toOption
      .flatMap(_.message.kycBridgeMessage)
      .flatMap(_.message.acuantProcessFinished)
  }

}
