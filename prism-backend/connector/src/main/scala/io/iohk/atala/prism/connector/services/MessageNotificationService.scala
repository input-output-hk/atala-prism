package io.iohk.atala.prism.connector.services

import java.util.concurrent.ConcurrentHashMap
import cats.effect._
import doobie.implicits._
import doobie.util.transactor.Transactor
import fs2.Stream
import io.iohk.atala.prism.connector.model.{Message, MessageId}
import io.iohk.atala.prism.connector.repositories.daos.MessagesDAO
import io.iohk.atala.prism.db.DbNotificationStreamer
import io.iohk.atala.prism.models.ParticipantId
import org.slf4j.{Logger, LoggerFactory}

import scala.util.{Failure, Success}
import cats.effect.Temporal
import cats.effect.std.Queue
import cats.effect.unsafe.IORuntime

class MessageNotificationService private (
    dbNotificationStreamer: DbNotificationStreamer,
    xa: Transactor[IO]
)(implicit runtime: IORuntime) {
  import MessageNotificationService._

  private val streamQueues =
    new ConcurrentHashMap[ParticipantId, Queue[IO, Option[Message]]]()

  private def enqueue(
      queue: Queue[IO, Option[Message]],
      v: Option[Message]
  ): Unit =
    queue.offer(v).unsafeRunSync()

  def stream(recipientId: ParticipantId): Stream[IO, Message] =
    for {
      queue <- Stream.eval(Queue.unbounded[IO, Option[Message]])
      _ <- Stream.eval {
        IO.delay {
          val oldQueue = Option(streamQueues.put(recipientId, queue))
          // Terminate the old queue to prevent its stream from hanging around forever
          oldQueue.foreach(enqueue(_, None))
        }
      }
      message <- Stream.fromQueueNoneTerminated(queue)
    } yield message

  def start(): Unit = {
    dbNotificationStreamer.stream
      .map { notification =>
        MessageId.from(notification.payload) match {
          case Success(notificationId) => Some(notificationId)
          case Failure(e) =>
            logger.error(
              s"DB notification payload could not be parsed as message ID",
              e
            )
            None
        }
      }
      // Ignore malformed IDs
      .unNone
      // Query whole message
      .evalMap(messageId =>
        MessagesDAO
          .getMessage(messageId)
          .map {
            case Some(message) => Some(message)
            case None =>
              logger.error(s"Message with ID $messageId not found")
              None
          }
          .transact(xa)
      )
      // Ignore messages not found
      .unNone
      // Notify any connected stream
      .map { message =>
        Option(streamQueues.get(message.recipientId)).foreach(
          enqueue(_, Some(message))
        )
      }
      .compile
      .drain
      .unsafeRunAsync {
        case Right(_) =>
          logger.info(
            "Closing all message streams because the DB notification stream has ended"
          )
          // The following two operations should ideally be atomic but, because the notification stream cannot be
          // restarted, we can ignore it
          streamQueues.values().forEach(enqueue(_, None))
          streamQueues.clear()
        case Left(e) =>
          // Only log failures when the service is not being shut down
          if (!dbNotificationStreamer.isStopped) {
            logger.error("Message stream from DB has failed", e)
          }
      }
  }

  def stop(): Unit = {
    dbNotificationStreamer.stopStreaming()
  }
}

object MessageNotificationService {
  private val logger: Logger = LoggerFactory.getLogger(getClass)

  def apply(
      xa: Transactor[IO]
  )(implicit
      timer: Temporal[IO],
      runtime: IORuntime
  ): MessageNotificationService = {
    val dbNotificationStreamer = DbNotificationStreamer("new_messages", xa)
    new MessageNotificationService(dbNotificationStreamer, xa)
  }

  def resourceAndStart(
      xa: Transactor[IO]
  )(implicit timer: Temporal[IO], runtime: IORuntime): Resource[IO, MessageNotificationService] =
    Resource.make(IO.delay {
      val service = MessageNotificationService(xa)
      service.start()
      service
    })(service => IO(service.stop()))
}
