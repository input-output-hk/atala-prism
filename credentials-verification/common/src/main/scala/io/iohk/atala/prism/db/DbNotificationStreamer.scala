package io.iohk.atala.prism.db

import java.util.concurrent.{CountDownLatch, TimeUnit}

import cats.effect._
import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.postgres._
import enumeratum.EnumEntry.Uppercase
import enumeratum.{Enum, EnumEntry}
import fs2.Stream
import fs2.Stream._
import io.circe.{Decoder, Json}
import monix.execution.atomic.AtomicBoolean
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.duration._

class DbNotificationStreamer private (channelName: String)(implicit timer: Timer[IO]) {
  import DbNotificationStreamer._

  // Derive `ConnectionIO` timer from `IO` timer
  private implicit val connectionIOTimer: Timer[ConnectionIO] = timer.mapK(LiftIO.liftK[ConnectionIO])

  private val stopped = AtomicBoolean(false)
  private val stoppedLatch = new CountDownLatch(1)

  lazy val stream: Stream[ConnectionIO, RowNotification] = {
    val notificationStream = for {
      // Grab channel as resource so it gets closed automatically when done
      _ <- resource(channel(channelName))
      // Sleep a bit between notification queries
      _ <- awakeEvery[ConnectionIO](100.millis)
      // Query DB notifications
      notifications <- eval(PHC.pgGetNotifications <* HC.commit)
      // Determine whether to stream down notifications or terminate
      maybeNotification <-
        if (stopped.get()) {
          // Stream has been told to shutdown, let the caller know
          stoppedLatch.countDown()
          // Use `None` to signal that the stream should terminate (`unNoneTerminate` is used below)
          emit(None)
        } else {
          // Push the new notifications down the stream
          emits(notifications.map(Some(_)))
        }
    } yield maybeNotification

    notificationStream.unNoneTerminate
      .map(notification =>
        io.circe.parser
          .parse(notification.getParameter)
          .flatMap(_.as[RowNotification])
          .leftMap(e => {
            logger.warn(s"Could not parse notification: ${notification.getParameter}", e)
            e
          })
          .toOption
      )
      // Ignore malformed notifications
      .unNone
  }

  def stopStreaming(): Unit = {
    logger.info(s"Stopping all open DB notification streams for channel $channelName")
    stopped.set(true)
    if (!stoppedLatch.await(1, TimeUnit.SECONDS)) {
      logger.warn(s"DB notification streams for channel $channelName could not stop gracefully on time")
    }
  }
}

object DbNotificationStreamer {
  private val logger: Logger = LoggerFactory.getLogger(getClass)

  private implicit val rowOperationDecoder: Decoder[RowOperation] = Decoder.decodeString.emapTry { string =>
    RowOperation.withNameInsensitiveEither(string).toTry
  }

  private implicit val rowNotificationDecoder: Decoder[RowNotification] = {
    Decoder.decodeJson.emapTry { json =>
      {
        val h = json.hcursor
        for {
          operation <- h.downField("operation").as[RowOperation]
          row <- h.downField("row").as[Json]
        } yield RowNotification(operation, row)
      }.toTry
    }
  }

  def apply(channelName: String)(implicit timer: Timer[IO]): DbNotificationStreamer = {
    new DbNotificationStreamer(channelName)
  }

  /** A resource that listens on a DB channel and unlistens when done. */
  private def channel(name: String): Resource[ConnectionIO, Unit] = {
    Resource.make(PHC.pgListen(name) *> HC.commit)(_ => PHC.pgUnlisten(name) *> HC.commit)
  }
}

sealed trait RowOperation extends EnumEntry
object RowOperation extends Enum[RowOperation] with Uppercase {
  val values: IndexedSeq[RowOperation] = findValues

  case object Insert extends RowOperation
  case object Update extends RowOperation
  case object Delete extends RowOperation
  case object Truncate extends RowOperation
}

case class RowNotification(operation: RowOperation, row: Json)
