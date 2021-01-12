package io.iohk.atala.prism.db

import cats.effect.{ContextShift, IO, Timer}
import doobie.HC
import doobie.implicits._
import doobie.postgres._
import io.iohk.atala.prism.db.DbNotificationStreamer.DbNotification
import io.iohk.atala.prism.repositories.PostgresRepositorySpec

import scala.concurrent.Future
import scala.concurrent.duration._

class DbNotificationStreamerSpec extends PostgresRepositorySpec {
  implicit val pc: PatienceConfig = PatienceConfig(10.seconds, 5.millis)

  private implicit val contextSwitch: ContextShift[IO] = IO.contextShift(ec)
  private implicit val timer: Timer[IO] = IO.timer(ec)

  private val CHANNEL = "test_channel"

  private def usingDbNotificationStreamer(f: DbNotificationStreamer => _): Unit = {
    val dbNotificationStreamer = DbNotificationStreamer(CHANNEL)
    try {
      f(dbNotificationStreamer)
    } finally {
      dbNotificationStreamer.stopStreaming()
    }
    ()
  }

  private def notify(payload: String): Unit = {
    val sendNotification = for {
      _ <- PHC.pgNotify(CHANNEL, payload)
      _ <- HC.commit
    } yield ()

    sendNotification.transact(database).unsafeRunSync()
  }

  private def streamSome(
      dbNotificationStreamer: DbNotificationStreamer,
      some: Int
  ): Future[List[DbNotification]] = {
    val stream = dbNotificationStreamer.stream
      .transact(database)
      .take(some.toLong)
      .timeout(5.seconds)
      .compile
      .toList
      .unsafeToFuture()

    // Give it some time to asynchronously connect to the DB
    Thread.sleep(100)

    stream
  }

  private def streamAll(dbNotificationStreamer: DbNotificationStreamer): Future[List[DbNotification]] = {
    val stream = dbNotificationStreamer.stream
      .transact(database)
      .drain
      .timeout(5.seconds)
      .compile
      .toList
      .unsafeToFuture()

    // Give it some time to asynchronously connect to the DB
    Thread.sleep(100)

    stream
  }

  "dbNotificationStreamer" should {
    "stream DB notifications" in {
      usingDbNotificationStreamer { dbNotificationStreamer =>
        val stream = streamSome(dbNotificationStreamer, some = 2)

        val notificationPayload1 = "Notification payload #1"
        notify(notificationPayload1)
        val notificationPayload2 = "Notification payload #2"
        notify(notificationPayload2)

        stream.futureValue mustBe List(DbNotification(notificationPayload1), DbNotification(notificationPayload2))
      }
    }

    "support multiple streams for the same channel" in {
      usingDbNotificationStreamer { dbNotificationStreamer =>
        val stream1 = streamSome(dbNotificationStreamer, some = 1)
        val stream2 = streamSome(dbNotificationStreamer, some = 1)

        val notificationPayload = "Notification payload"
        notify(notificationPayload)

        val expectedNotifications = List(DbNotification(notificationPayload))
        stream1.futureValue mustBe expectedNotifications
        stream2.futureValue mustBe expectedNotifications
      }
    }

    "stop streaming when stopped" in {
      usingDbNotificationStreamer { dbNotificationStreamer =>
        val stream = streamAll(dbNotificationStreamer)

        dbNotificationStreamer.stopStreaming()

        // Really only care about completing the future
        stream.futureValue mustBe empty
      }
    }
  }
}
