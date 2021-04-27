package io.iohk.atala.prism.services

import scala.annotation.tailrec
import scala.concurrent.duration.DurationInt
import fs2.Stream
import io.grpc.Status
import io.iohk.atala.prism.errors.PrismError
import monix.eval.Task
import org.slf4j.LoggerFactory
import io.iohk.atala.prism.models.ConnectorMessageId
import io.iohk.atala.prism.protos.connector_api.{SendMessageRequest, SendMessageResponse}
import io.iohk.atala.prism.protos.connector_models.ReceivedMessage
import io.iohk.atala.prism.protos.credential_models.AtalaMessage
import io.iohk.atala.prism.services.ConnectorMessagesService.MessageProcessorNotFound
import io.iohk.atala.prism.services.MessageProcessor.MessageProcessorResult

trait MessageProcessor extends (ReceivedMessage => Option[MessageProcessorResult])
object MessageProcessor {
  type MessageProcessorResult = Task[Either[PrismError, Option[AtalaMessage]]]
  def successful(message: Option[AtalaMessage] = None): MessageProcessorResult = Task.pure(Right(message))
  def failed(error: PrismError): MessageProcessorResult = Task.pure(Left(error))
}

class ConnectorMessagesService(
    connectorService: ConnectorClientService,
    messageProcessors: List[MessageProcessor] = List.empty,
    findLastMessageOffset: Task[Option[ConnectorMessageId]],
    saveMessageOffset: ConnectorMessageId => Task[Unit]
) {

  private val logger = LoggerFactory.getLogger(classOf[ConnectorMessagesService])

  private val GET_MESSAGES_PAGINATED_LIMIT = 100
  private val GET_MESSAGES_PAGINATED_AWAKE_DELAY = 10.seconds

  val messagesUpdatesStream: Stream[Task, Unit] = {
    Stream
      .eval(findLastMessageOffset)
      .flatMap { lastSeenMessageId =>
        connectorService.getMessagesPaginatedStream(
          lastSeenMessageId,
          GET_MESSAGES_PAGINATED_LIMIT,
          GET_MESSAGES_PAGINATED_AWAKE_DELAY
        )
      }
      .flatMap(Stream.emits)
      .evalTap { receivedMessage =>
        for {
          result <- tryToProcessMessage(messageProcessors, receivedMessage).getOrElse(
            MessageProcessor.failed(MessageProcessorNotFound(receivedMessage))
          )

          _ <- result match {
            case Left(error) =>
              logger.warn(s"Error occurred during processing of message: ${error.toStatus.getDescription}")
              sendResponseMessage(error.toAtalaMessage, receivedMessage)
            case Right(None) => Task.unit
            case Right(Some(message)) => sendResponseMessage(message, receivedMessage)
          }
          _ <- saveMessageOffset(ConnectorMessageId(receivedMessage.id))
        } yield ()
      }
      .drain
  }

  private def sendResponseMessage(
      message: AtalaMessage,
      receivedMessage: ReceivedMessage
  ): Task[SendMessageResponse] = {
    val atalaMessage = message.withReplyTo(receivedMessage.id)
    val sendMessageRequest = SendMessageRequest(
      connectionId = receivedMessage.connectionId,
      message = atalaMessage.toByteString
    )
    connectorService.sendMessage(sendMessageRequest)
  }

  @tailrec
  private def tryToProcessMessage(
      messageProcessors: List[MessageProcessor],
      receivedMessage: ReceivedMessage
  ): Option[MessageProcessorResult] = {
    messageProcessors match {
      case head :: tail =>
        head(receivedMessage) match {
          case None => tryToProcessMessage(tail, receivedMessage)
          case task => task
        }
      case Nil => None
    }
  }
}

object ConnectorMessagesService {
  case class MessageProcessorNotFound(receivedMessage: ReceivedMessage) extends PrismError {
    override def toStatus: Status = {
      Status.UNIMPLEMENTED.withDescription(
        s"Connector message with id: ${receivedMessage.id} and content: ${receivedMessage.message.toString} " +
          "cannot be processed by any processor, skipping it"
      )
    }
  }
}
