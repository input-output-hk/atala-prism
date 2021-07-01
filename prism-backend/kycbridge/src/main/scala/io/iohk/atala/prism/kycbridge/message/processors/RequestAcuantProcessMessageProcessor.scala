package io.iohk.atala.prism.kycbridge.message.processors

import java.time.Instant

import scala.util.Try

import io.circe.syntax._
import cats.syntax.functor._

import io.iohk.atala.prism.kycbridge.task.lease.system.KycBridgeProcessingTaskState
import io.iohk.atala.prism.kycbridge.task.lease.system.data.AcuantStartProcessForConnectionData
import io.iohk.atala.prism.protos.connector_models.ReceivedMessage
import io.iohk.atala.prism.protos.credential_models.{RequestAcuantProcess, AtalaMessage}
import io.iohk.atala.prism.services.MessageProcessor
import io.iohk.atala.prism.task.lease.system.{ProcessingTaskData, ProcessingTaskService}

class RequestAcuantProcessMessageProcessor(
    processingTaskService: ProcessingTaskService[KycBridgeProcessingTaskState]
) {

  val processor: MessageProcessor = { receivedMessage =>
    parseRequestAcuantProcessMessage(receivedMessage)
      .as {
        val data = AcuantStartProcessForConnectionData(
          receivedMessageId = receivedMessage.id,
          connectionId = receivedMessage.connectionId
        )

        val processingTaskData = ProcessingTaskData(data.asJson)

        processingTaskService
          .create(processingTaskData, KycBridgeProcessingTaskState.AcuantStartProcessForConnection, Instant.now())
          .flatMap(_ => MessageProcessor.successful())
      }
  }

  private[processors] def parseRequestAcuantProcessMessage(
      message: ReceivedMessage
  ): Option[RequestAcuantProcess] = {
    Try(AtalaMessage.parseFrom(message.message.toByteArray)).toOption
      .flatMap(_.message.kycBridgeMessage)
      .flatMap(_.message.requestAcuantProcess)
  }

}
