package io.iohk.atala.prism.db

import java.util.concurrent.{CountDownLatch, TimeUnit}

import cats.effect._
import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.postgres._
import fs2.Stream
import fs2.Stream._
import monix.execution.atomic.AtomicBoolean
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.duration._

class DbNotificationStreamer private (channelName: String)(implicit
    timer: Timer[IO]
) {
  import DbNotificationStreamer._

  // Derive `ConnectionIO` timer from `IO` timer
  private implicit val connectionIOTimer: Timer[ConnectionIO] =
    timer.mapK(LiftIO.liftK[ConnectionIO])

  private val stopped = AtomicBoolean(false)
  private val stoppedLatch = new CountDownLatch(1)

  lazy val stream: Stream[ConnectionIO, DbNotification] = {
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

    notificationStream.unNoneTerminate.map(notification => DbNotification(payload = notification.getParameter))
  }

  def isStopped: Boolean = stopped.get()

  def stopStreaming(): Unit = {
    logger.info(
      s"Stopping all open DB notification streams for channel $channelName"
    )
    stopped.set(true)
    if (!stoppedLatch.await(1, TimeUnit.SECONDS)) {
      logger.warn(
        s"DB notification streams for channel $channelName could not stop gracefully on time"
      )
    }
  }
}

object DbNotificationStreamer {
  private val logger: Logger = LoggerFactory.getLogger(getClass)

  def apply(
      channelName: String
  )(implicit timer: Timer[IO]): DbNotificationStreamer = {
    new DbNotificationStreamer(channelName)
  }

  /** A resource that listens on a DB channel and unlistens when done. */
  private def channel(name: String): Resource[ConnectionIO, Unit] = {
    Resource.make(PHC.pgListen(name) *> HC.commit)(_ => PHC.pgUnlisten(name) *> HC.commit)
  }

  case class DbNotification(payload: String)
}
