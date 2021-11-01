package io.iohk.atala.prism.db

import cats.effect._
import cats.effect.unsafe.IORuntime
import cats.implicits._
import cats.~>
import doobie._
import doobie.implicits._
import doobie.postgres._
import fs2.{Pipe, Stream}
import fs2.Stream._
import org.postgresql.PGNotification
import org.slf4j.{Logger, LoggerFactory}

import java.util.concurrent.{CountDownLatch, TimeUnit}
import scala.concurrent.duration._
import cats.effect.{ Ref, Temporal }

class DbNotificationStreamer private (channelName: String, xa: Transactor[IO])(implicit temporal: Temporal[IO], runtime: IORuntime) {
  import DbNotificationStreamer._

  private val stopped = Ref.unsafe[IO, Boolean](false)
  private val stoppedLatch = new CountDownLatch(1)

  lazy val stream: Stream[IO, DbNotification] = {
    def inner(liftToConnIO: IO ~> ConnectionIO): Pipe[ConnectionIO, FiniteDuration, Option[PGNotification]] =
      ticks => for {
        // Grab channel as resource so it gets closed automatically when done
        _ <- resource(channel(channelName))
        // Sleep a bit between notification queries
        _ <- ticks
        // Query DB notifications
        notifications <- eval(PHC.pgGetNotifications <* HC.commit)
        // Determine whether to stream down notifications or terminate
        isStopped <- Stream.eval(liftToConnIO(stopped.get))
        maybeNotification <-
          if (isStopped) {
            // Stream has been told to shutdown, let the caller know
            stoppedLatch.countDown()
            // Use `None` to signal that the stream should terminate (`unNoneTerminate` is used below)
            emit[ConnectionIO, Option[PGNotification]](None)
          } else {
            // Push the new notifications down the stream
            emits[ConnectionIO, Option[PGNotification]](notifications.map(Option(_)))
          }
      } yield maybeNotification

    val notificationStream = for {
      liftToConnIO <- resource(WeakAsync.liftK[IO, ConnectionIO])
      stream <- awakeEvery[IO](100.millis).through(inner(liftToConnIO).transact(xa))
    } yield stream

    notificationStream.unNoneTerminate.map(notification => DbNotification(payload = notification.getParameter))
  }

  def isStopped: Boolean = stopped.get.unsafeRunSync()

  def stopStreaming(): Unit = {
    logger.info(
      s"Stopping all open DB notification streams for channel $channelName"
    )
    stopped.set(true).unsafeRunSync()
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
      channelName: String,
      xa: Transactor[IO]
  )(implicit temporal: Temporal[IO], runtime: IORuntime): DbNotificationStreamer = {
    new DbNotificationStreamer(channelName, xa)
  }

  /** A resource that listens on a DB channel and unlistens when done. */
  private def channel(name: String): Resource[ConnectionIO, Unit] = {
    Resource.make(PHC.pgListen(name) *> HC.commit)(_ => PHC.pgUnlisten(name) *> HC.commit)
  }

  case class DbNotification(payload: String)
}
