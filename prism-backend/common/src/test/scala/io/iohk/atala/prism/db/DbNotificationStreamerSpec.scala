package io.iohk.atala.prism.db

import cats.effect.kernel.Outcome
import cats.effect.{IO, OutcomeIO}
import cats.effect.kernel.Outcome.Succeeded
import cats.effect.unsafe.implicits.global
import doobie.HC
import doobie.implicits._
import doobie.postgres._
import fs2.INothing
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.db.DbNotificationStreamer.DbNotification
import org.scalatest.{Assertion, Assertions}
import org.slf4j.{Logger, LoggerFactory}

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

  private def streamSome[A](
      dbNotificationStreamer: DbNotificationStreamer,
      some: Int
  )(f: IO[OutcomeIO[List[DbNotification]]] => IO[A]): IO[A] =
    dbNotificationStreamer.stream
      .take(some.toLong)
      .timeout(5.seconds)
      .compile
      .toList
      .background
      .use(streamOutcomeIo => IO.sleep(2.seconds) *> f(streamOutcomeIo))

  private def streamAll[A](
      dbNotificationStreamer: DbNotificationStreamer
  )(f: IO[OutcomeIO[List[INothing]]] => IO[A]): IO[A] =
    dbNotificationStreamer.stream.drain
      .timeout(5.seconds)
      .compile
      .toList
      .background
      .use(streamOutcomeIo => IO.sleep(2.seconds) *> f(streamOutcomeIo))

  private implicit class OutcomeIOOps[A](outcomeIO: OutcomeIO[A]) {
    def mustBeIO(a: A): IO[Assertion] =
      outcomeIO match {
        case Succeeded(fa) => fa.map(_ mustBe a)
        case Outcome.Errored(e) => Assertions.fail("Background effect was not completed successfully", e)
        case Outcome.Canceled() => IO(Assertions.fail("Background effect was cancelled"))
      }
  }
// Todo, Kamil fix it
  "dbNotificationStreamer" should {
    "stream DB notifications" in {
      usingDbNotificationStreamer { dbNotificationStreamer =>
        streamSome(dbNotificationStreamer, some = 2) { streamOutcomeIo =>
          val notificationPayload1 = "Notification payload #1"
          val notificationPayload2 = "Notification payload #2"
          for {
            _ <- notify(notificationPayload1)
            _ <- notify(notificationPayload2)
            actual <- streamOutcomeIo
            expected = List(DbNotification(notificationPayload1), DbNotification(notificationPayload2))
            _ <- actual mustBeIO expected
          } yield ()
        }
      }
    }

    "support multiple streams for the same channel" in {
      usingDbNotificationStreamer { dbNotificationStreamer =>
        streamSome(dbNotificationStreamer, some = 1) { streamOutcomeIo1 =>
          streamSome(dbNotificationStreamer, some = 1) { streamOutcomeIo2 =>
            val notificationPayload = "Notification payload"
            for {
              _ <- notify(notificationPayload)

              expected = List(DbNotification(notificationPayload))
              actual1 <- streamOutcomeIo1
              actual2 <- streamOutcomeIo2
              _ <- actual1 mustBeIO expected
              _ <- actual2 mustBeIO expected
            } yield ()
          }
        }
      }
    }

    "stop streaming when stopped" in {
      usingDbNotificationStreamer { dbNotificationStreamer =>
        streamAll(dbNotificationStreamer) { streamOutcomeIo =>
          dbNotificationStreamer.stopStreaming()
          for {
            actual <- streamOutcomeIo
            // Really only care about completing the effect
            _ <- actual mustBeIO List.empty
          } yield ()
        }
      }
    }
  }
}
