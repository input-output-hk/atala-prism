package io.iohk.cef.utils
import scala.concurrent.duration.Duration

import scala.language.implicitConversions

object JavaTimeConversions {
  implicit def javaDurationToScalaDuration(jd: java.time.Duration): Duration = {
    Duration.fromNanos(jd.toNanos)
  }
}
