package io.iohk.atala.mirror.services

import io.iohk.atala.mirror.models.{CardanoAddress, LovelaceAmount, TrisaVaspAddress}
import io.iohk.atala.prism.protos.connector_models.ReceivedMessage
import io.iohk.atala.prism.protos.credential_models.{AtalaMessage, InitiateTrisaCardanoTransactionMessage}
import io.iohk.atala.prism.services.MessageProcessor
import cats.implicits._
import io.iohk.atala.prism.services.MessageProcessor.MessageProcessorResult

import scala.util.Try

class TrisaService(trisaIntegrationService: TrisaIntegrationService) {

  val initiateTrisaCardanoTransactionMessageProcessor: MessageProcessor = { receivedMessage =>
    parseInitiateTrisaCardanoTransactionMessage(receivedMessage)
      .map(processInitiateTrisaCardanoTransactionMessage)
  }

  private def parseInitiateTrisaCardanoTransactionMessage(
      message: ReceivedMessage
  ): Option[InitiateTrisaCardanoTransactionMessage] = {
    Try(AtalaMessage.parseFrom(message.message.toByteArray)).toOption
      .flatMap(_.message.mirrorMessage)
      .flatMap(_.message.initiateTrisaCardanoTransactionMessage)
  }

  private def processInitiateTrisaCardanoTransactionMessage(
      message: InitiateTrisaCardanoTransactionMessage
  ): MessageProcessorResult = {
    trisaIntegrationService
      .initiateTransaction(
        CardanoAddress(message.sourceCardanoAddress),
        CardanoAddress(message.desinationCardanoAddress),
        LovelaceAmount(message.lovelaceAmount),
        trisaVaspAddress = TrisaVaspAddress(message.trisaVaspHost, message.trisaVaspHostPortNumber)
      )
      .map(_.as(None))
  }
}
