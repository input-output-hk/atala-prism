package io.iohk.atala.prism.utils

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import io.iohk.atala.prism.utils.syntax._

import java.time.Instant
import scala.util.Random

class UtilsSpec extends AnyWordSpec with Matchers {

  "Instant and Timestamp ops" should {
    "do a proper conversion" in {
      val seconds = Random.between(0, 300L)
      val nanos = Random.between(0, 300L)

      val instant = Instant.ofEpochSecond(seconds, nanos)

      val timeStampFromInstant = instant.toProtoTimestamp

      timeStampFromInstant.seconds mustBe seconds
      timeStampFromInstant.nanos.toLong mustBe nanos
      timeStampFromInstant.toInstant mustBe instant
    }
  }
}
