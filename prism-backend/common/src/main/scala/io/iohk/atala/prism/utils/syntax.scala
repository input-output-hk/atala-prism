package io.iohk.atala.prism.utils

import com.google.protobuf.timestamp.Timestamp
import java.time.Instant
import scala.concurrent.Future
import scala.util.Try

object syntax {
  implicit class SyntaxOps[A](exp: => A) {

    /** returns a Future containing the value without creating a new thread
      */
    def tryF: Future[A] = Future.fromTry(Try { exp })
  }

  implicit class InstantToTimestampOps(val value: Instant) extends AnyVal {

    /** converts instant to proto timestamp */
    def toProtoTimestamp: Timestamp = Timestamp(value.getEpochSecond, value.getNano)
  }

  implicit class TimestampOps(val value: Timestamp) extends AnyVal {

    /** converts timestamp to instant using seconds value */
    def toInstant: Instant = Instant.ofEpochSecond(value.seconds, value.nanos.toLong)
  }
}
