package io.iohk.atala.prism.db

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import doobie.HC
import doobie.implicits._
import doobie.postgres._
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.db.DbNotificationStreamer.DbNotification

import scala.concurrent.duration._

class DbNotificationStreamerSpec extends AtalaWithPostgresSpec {
  private val CHANNEL = "test_channel"

  private def usingDbNotificationStreamer[A](
      f: DbNotificationStreamer => IO[A]
  ): Unit = {
    val dbNotificationStreamer = DbNotificationStreamer(CHANNEL, db)
    try {
      f(dbNotificationStreamer).unsafeRunSync()
    } finally {
      dbNotificationStreamer.stopStreaming()
    }
    ()
  }

  private def notify(payload: String): IO[Unit] = {
    val sendNotification = for {
      _ <- PHC.pgNotify(CHANNEL, payload)
      _ <- HC.commit
    } yield ()

    sendNotification.transact(database)
  }

  private def streamSome(
      stream: fs2.Stream[IO, DbNotification],
      some: Int
  ): IO[List[DbNotification]] =
    stream
      .take(some.toLong)
      .timeout(5.seconds)
      .compile
      .toList

  private def streamAll(
      stream: fs2.Stream[IO, DbNotification]
  ): IO[List[DbNotification]] =
    stream
      .drain
      .timeout(5.seconds)
      .compile
      .toList

  "dbNotificationStreamer" should {
    "stream DB notifications" in {
      usingDbNotificationStreamer { dbNotificationStreamer =>
        for {
          stream <- dbNotificationStreamer.stream
          notificationPayload1 = "Notification payload #1"
          notificationPayload2 = "Notification payload #2"
          _ <- notify(notificationPayload1)
          _ <- notify(notificationPayload2)
          result <- streamSome(stream, some = 2)
          _ = result mustBe List(
            DbNotification(notificationPayload1),
            DbNotification(notificationPayload2)
          )
        } yield ()
      }
    }

    "support multiple streams for the same channel" in {
      usingDbNotificationStreamer { dbNotificationStreamer =>
        for {
          stream1 <- dbNotificationStreamer.stream
          stream2 <- dbNotificationStreamer.stream

          notificationPayload = "Notification payload"
          _ <- notify(notificationPayload)

          expectedNotifications = List(DbNotification(notificationPayload))
          result1 <- streamSome(stream1, some = 1)
          result2 <- streamSome(stream2, some = 1)
          _ = result1 mustBe expectedNotifications
          _ = result2 mustBe expectedNotifications
        } yield ()
      }
    }

    "stop streaming when stopped" in {
      usingDbNotificationStreamer { dbNotificationStreamer =>
        for {
          stream <- dbNotificationStreamer.stream
          _ = dbNotificationStreamer.stopStreaming()
          result <- streamAll(stream)
          // Really only care about completing the effect
          _ = result mustBe empty
        } yield ()
      }
    }
  }
}
