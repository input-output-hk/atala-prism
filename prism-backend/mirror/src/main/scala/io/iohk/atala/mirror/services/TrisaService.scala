package io.iohk.atala.mirror.services

import io.iohk.atala.mirror.models.{CardanoAddress, LovelaceAmount, TrisaVaspAddress}
import io.iohk.atala.prism.protos.connector_models.ReceivedMessage
import io.iohk.atala.prism.protos.credential_models.{AtalaMessage, InitiateTrisaCardanoTransactionMessage}
import io.iohk.atala.prism.services.MessageProcessor
import io.iohk.atala.prism.services.MessageProcessor.MessageProcessorException
import monix.eval.Task
import cats.implicits._
import org.slf4j.LoggerFactory

import scala.util.Try

class TrisaService(trisaIntegrationService: TrisaIntegrationService) {

  private val logger = LoggerFactory.getLogger(classOf[TrisaService])

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
  ): Task[Either[MessageProcessorException, Unit]] = {
    trisaIntegrationService
      .initiateTransaction(
        CardanoAddress(message.sourceCardanoAddress),
        CardanoAddress(message.desinationCardanoAddress),
        LovelaceAmount(message.lovelaceAmount),
        trisaVaspAddress = TrisaVaspAddress(message.trisaVaspHost, message.trisaVaspHostPortNumber)
      )
      .map { result =>
        result.leftMap(e => new MessageProcessorException(e.getMessage)).as {
          logger.info(s"Transaction to trisa vasp send successfully")
        }
      }
  }

}
