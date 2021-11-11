package io.iohk.atala.prism.utils

import cats.syntax.applicativeError._
import com.google.protobuf.timestamp.Timestamp
import doobie.implicits._
import org.slf4j.{Logger, LoggerFactory}

import java.time.Instant
import scala.concurrent.Future
import scala.util.Try
import io.iohk.atala.prism.repositories.ConnectionIOErrorHandlers

object syntax {

  lazy val logger: Logger = LoggerFactory.getLogger(this.getClass)

  implicit class SyntaxOps[A](exp: => A) {

    /** returns a Future containing the value without creating a new thread
      */
    def tryF: Future[A] = Future.fromTry(Try { exp })
  }

  implicit class InstantToTimestampOps(val value: Instant) extends AnyVal {

    /** converts instant to proto timestamp */
    def toProtoTimestamp: Timestamp =
      Timestamp(value.getEpochSecond, value.getNano)
  }

  implicit class TimestampOps(val value: Timestamp) extends AnyVal {

    /** converts timestamp to instant using seconds value */
    def toInstant: Instant =
      Instant.ofEpochSecond(value.seconds, value.nanos.toLong)
  }

  implicit class LongOps(val value: Long) extends AnyVal {

    /** converts long to instant using milli value */
    def toInstant: Instant = Instant.ofEpochMilli(value)
  }

  implicit class DBConnectionOps[T](val connection: doobie.ConnectionIO[T]) extends AnyVal {

    /** logs SQL errors from DB, to not expose them to the user */
    def logSQLErrorsV2(
        operationDescription: => String,
        onError: => doobie.ConnectionIO[T] = new RuntimeException(
          "Unexpected SQL error, please fix before creating PR"
        ).raiseError[doobie.ConnectionIO, T]
    ): doobie.ConnectionIO[T] =
      ConnectionIOErrorHandlers.handleSQLErrors(
        connection,
        logger,
        operationDescription,
        onError
      )
  }

  implicit class EitherThrowableOps[R](val in: Either[Throwable, R]) extends AnyVal {

    /** converts Either[Throwable, T] into Future */
    def toFuture: Future[R] = in.fold(Future.failed, Future.successful)
  }
}
