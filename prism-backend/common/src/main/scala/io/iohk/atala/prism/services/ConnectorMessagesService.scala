package io.iohk.atala.prism.services

import scala.annotation.tailrec
import scala.concurrent.duration.DurationInt

import fs2.Stream
import monix.eval.Task
import org.slf4j.LoggerFactory

import io.iohk.atala.prism.models.ConnectorMessageId
import io.iohk.atala.prism.protos.connector_models.ReceivedMessage
import io.iohk.atala.prism.services.MessageProcessor.{MessageProcessorResult, MessageProcessorException}

trait MessageProcessor extends (ReceivedMessage => Option[MessageProcessorResult])
object MessageProcessor {
  type MessageProcessorResult = Task[Either[MessageProcessorException, Unit]]
  def successful: MessageProcessorResult = Task.pure(Right(()))
  def failed(error: MessageProcessorException): MessageProcessorResult = Task.pure(Left(error))

  case class MessageProcessorException(message: String, cause: Option[Throwable] = None)
      extends Exception(message, cause.orNull)
  object MessageProcessorException {

    /**
      * Create a [[MessageProcessorException]] from a [[Throwable]].
      */
    def apply(cause: Throwable): MessageProcessorException =
      MessageProcessorException(cause.getMessage, cause = Some(cause))
  }
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
            MessageProcessor.failed(
              MessageProcessorException(
                s"Connector message with id: ${receivedMessage.id} and content: ${receivedMessage.message.toString} " +
                  "cannot be processed by any processor, skipping it"
              )
            )
          )

          _ <- result match {
            case Left(error) => Task(logger.warn(error.getMessage))
            case Right(_) => Task.unit
          }
          _ <- saveMessageOffset(ConnectorMessageId(receivedMessage.id))
        } yield ()
      }
      .drain
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
