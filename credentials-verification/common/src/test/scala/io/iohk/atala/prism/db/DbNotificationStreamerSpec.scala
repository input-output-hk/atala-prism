package io.iohk.atala.prism.db

import cats.effect.{ContextShift, IO, Timer}
import doobie.HC
import doobie.implicits._
import doobie.postgres._
import io.circe.Json
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

  private def notify(json: Json): Unit = notify(json.noSpaces)
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
  ): Future[List[RowNotification]] = {
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

  private def streamAll(dbNotificationStreamer: DbNotificationStreamer): Future[List[RowNotification]] = {
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
        val stream = streamSome(dbNotificationStreamer, some = 4)

        val insertRow = Json.obj("test" -> Json.fromString("inserted"))
        notify(Json.obj("operation" -> Json.fromString("INSERT"), "row" -> insertRow))
        val updateRow = Json.obj("test" -> Json.fromString("updated"))
        notify(Json.obj("operation" -> Json.fromString("UPDATE"), "row" -> updateRow))
        val deleteRow = Json.obj("test" -> Json.fromString("deleted"))
        notify(Json.obj("operation" -> Json.fromString("DELETE"), "row" -> deleteRow))
        val truncateRow = Json.obj("test" -> Json.fromString("truncated"))
        notify(Json.obj("operation" -> Json.fromString("TRUNCATE"), "row" -> truncateRow))

        stream.futureValue mustBe
          List(
            RowNotification(RowOperation.Insert, insertRow),
            RowNotification(RowOperation.Update, updateRow),
            RowNotification(RowOperation.Delete, deleteRow),
            RowNotification(RowOperation.Truncate, truncateRow)
          )
      }
    }

    "support multiple streams for the same channel" in {
      usingDbNotificationStreamer { dbNotificationStreamer =>
        val stream1 = streamSome(dbNotificationStreamer, some = 2)
        val stream2 = streamSome(dbNotificationStreamer, some = 2)

        val insertRow = Json.obj("test" -> Json.fromString("inserted"))
        notify(Json.obj("operation" -> Json.fromString("INSERT"), "row" -> insertRow))
        val updateRow = Json.obj("test" -> Json.fromString("updated"))
        notify(Json.obj("operation" -> Json.fromString("UPDATE"), "row" -> updateRow))

        val expectedNotifications =
          List(RowNotification(RowOperation.Insert, insertRow), RowNotification(RowOperation.Update, updateRow))
        stream1.futureValue mustBe expectedNotifications
        stream2.futureValue mustBe expectedNotifications
      }
    }

    "ignore malformed DB notifications" in {
      usingDbNotificationStreamer { dbNotificationStreamer =>
        val stream = streamSome(dbNotificationStreamer, some = 2)

        val insertRow = Json.obj("test" -> Json.fromString("inserted"))
        notify(Json.obj("operation" -> Json.fromString("INSERT"), "row" -> insertRow))
        // Should be ignored
        notify(
          Json.obj("operation-wrong" -> Json.fromString("INSERT"), "row" -> Json.obj("test" -> Json.fromBoolean(true)))
        )
        // Should be ignored
        notify(
          Json.obj("operation" -> Json.fromString("INSERT"), "row-wrong" -> Json.obj("test" -> Json.fromBoolean(true)))
        )
        // Should be ignored
        notify("not-json")
        val updateRow = Json.obj("test" -> Json.fromString("updated"))
        notify(Json.obj("operation" -> Json.fromString("UPDATE"), "row" -> updateRow))

        stream.futureValue mustBe List(
          RowNotification(RowOperation.Insert, insertRow),
          RowNotification(RowOperation.Update, updateRow)
        )
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
