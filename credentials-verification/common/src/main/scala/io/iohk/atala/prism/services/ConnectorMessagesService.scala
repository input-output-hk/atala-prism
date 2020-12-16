package io.iohk.atala.prism.services

import fs2.Stream
import io.iohk.atala.prism.protos.connector_models.ReceivedMessage
import monix.eval.Task

import scala.concurrent.duration.DurationInt
import io.iohk.atala.prism.models.ConnectorMessageId
import org.slf4j.LoggerFactory

import scala.annotation.tailrec

trait MessageProcessor {
  def attemptProcessMessage(receivedMessage: ReceivedMessage): Option[Task[Unit]]
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
          _ <- tryToProcessMessage(messageProcessors, receivedMessage).getOrElse {
            logger.warn(
              s"Connector message with id: ${receivedMessage.id} and content: ${receivedMessage.message.toString} " +
                s"cannot be processed by any processor, skipping it"
            )
            Task.unit
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
  ): Option[Task[Unit]] = {
    messageProcessors match {
      case head :: tail =>
        head.attemptProcessMessage(receivedMessage) match {
          case None => tryToProcessMessage(tail, receivedMessage)
          case task => task
        }
      case Nil => None
    }
  }
}
