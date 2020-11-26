package io.iohk.atala.prism.connector.services

import java.time.ZonedDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

import cats.effect._
import doobie.implicits._
import doobie.util.transactor.Transactor
import fs2.Stream
import fs2.concurrent.{NoneTerminatedQueue, Queue}
import io.circe.Decoder
import io.iohk.atala.prism.connector.model.{ConnectionId, Message, MessageId}
import io.iohk.atala.prism.db.DbNotificationStreamer
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.util.BytesOps
import org.slf4j.{Logger, LoggerFactory}

import scala.util.Try

class MessageNotificationService private (
    dbNotificationStreamer: DbNotificationStreamer,
    xa: Transactor[IO]
)(implicit
    contextShift: ContextShift[IO]
) {
  import MessageNotificationService._

  private val streamQueues = new ConcurrentHashMap[ParticipantId, NoneTerminatedQueue[IO, Message]]()

  private def enqueue(queue: NoneTerminatedQueue[IO, Message], v: Option[Message]): Unit = {
    ConcurrentEffect[IO].runAsync(queue.enqueue1(v))(_ => IO.unit).unsafeRunSync()
  }

  def stream(recipientId: ParticipantId): Stream[IO, Message] = {
    for {
      queue <- Stream.eval(Queue.noneTerminated[IO, Message])
      _ <- Stream.eval {
        IO.delay {
          val oldQueue = Option(streamQueues.put(recipientId, queue))
          // Terminate the old queue to prevent its stream from hanging around forever
          oldQueue.foreach(enqueue(_, None))
        }
      }
      message <- queue.dequeue
    } yield message
  }

  def start(): Unit = {
    dbNotificationStreamer.stream
      .map { notification =>
        val row = notification.row
        val recipientId = row.hcursor.downField("recipient").as[UUID].map(ParticipantId(_)).toOption
        recipientId
          .flatMap(r => Option(streamQueues.get(r)))
          .foreach { queue =>
            row.as[Message].toOption.foreach(message => enqueue(queue, Some(message)))
          }
      }
      .transact(xa)
      .compile
      .drain
      .unsafeRunAsync {
        case Right(_) =>
          logger.info("Closing all message streams because the DB notification stream has ended")
          // The following two operations should ideally be atomic but, because the notification stream cannot be
          // restarted, we can ignore it
          streamQueues.values().forEach(enqueue(_, None))
          streamQueues.clear()
        case Left(e) => logger.error("Message stream from DB has failed", e)
      }
  }

  def stop(): Unit = {
    dbNotificationStreamer.stopStreaming()
  }
}

object MessageNotificationService {
  private val logger: Logger = LoggerFactory.getLogger(getClass)

  private implicit val messageDecoder: Decoder[Message] = {
    Decoder.decodeJson.emapTry { json =>
      {
        val h = json.hcursor
        for {
          id <- h.downField("id").as[UUID]
          connection <- h.downField("connection").as[UUID]
          // Instant format from DB has timezone
          receivedAt <- h.downField("received_at").as[ZonedDateTime]
          hexContent <- h.downField("content").as[String]
          content <- Try(BytesOps.hexToBytes(hexContent.stripPrefix("\\x"))).toEither
        } yield Message(MessageId(id), ConnectionId(connection), receivedAt.toInstant, content)
      }.toTry
    }
  }

  def apply(
      xa: Transactor[IO]
  )(implicit contextShift: ContextShift[IO], timer: Timer[IO]): MessageNotificationService = {
    val dbNotificationStreamer = DbNotificationStreamer("new_messages")
    new MessageNotificationService(dbNotificationStreamer, xa)
  }
}
