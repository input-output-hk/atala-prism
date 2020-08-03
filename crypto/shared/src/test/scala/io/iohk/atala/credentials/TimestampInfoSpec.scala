package io.iohk.atala.credentials

import java.time.Instant

import org.scalacheck.Gen
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks._

class TimestampInfoSpec extends AnyWordSpec {
  // Use a max value to guarantee time operations are always valid
  private val MAX_INT = 1000
  private val intGen = Gen.chooseNum(1, MAX_INT)
  private val timeGen = intGen.map(millis => Instant.ofEpochMilli(MAX_INT + millis))

  "occurredBefore" should {
    "return true when atalaBlockTimestamp is before" in {
      forAll(timeGen, intGen, intGen, intGen, intGen, intGen) { (atalaBlockTimestamp, timeOffset, a, b, c, d) =>
        val x = TimestampInfo(atalaBlockTimestamp, a, b)
        val y = TimestampInfo(atalaBlockTimestamp.plusMillis(timeOffset), c, d)
        x occurredBefore y must be(true)
      }
    }

    "return false when atalaBlockTimestamp is later" in {
      forAll(timeGen, intGen, intGen, intGen, intGen, intGen) { (atalaBlockTimestamp, timeOffset, a, b, c, d) =>
        val x = TimestampInfo(atalaBlockTimestamp, a, b)
        val y = TimestampInfo(atalaBlockTimestamp.minusMillis(timeOffset), c, d)
        x occurredBefore y must be(false)
      }
    }

    "return true when atalaBlockTimestamp is the same and atalaBlockSequenceNumber is smaller" in {
      forAll(timeGen, intGen, intGen, intGen, intGen) {
        (atalaBlockTimestamp, atalaBlockSequenceNumber, blockOffset, a, b) =>
          val x = TimestampInfo(atalaBlockTimestamp, atalaBlockSequenceNumber, a)
          val y = TimestampInfo(atalaBlockTimestamp, atalaBlockSequenceNumber + blockOffset, b)
          x occurredBefore y must be(true)
      }
    }

    "return false when atalaBlockTimestamp is the same and atalaBlockSequenceNumber is larger" in {
      forAll(timeGen, intGen, intGen, intGen, intGen) {
        (atalaBlockTimestamp, atalaBlockSequenceNumber, blockOffset, a, b) =>
          val x = TimestampInfo(atalaBlockTimestamp, atalaBlockSequenceNumber, a)
          val y = TimestampInfo(atalaBlockTimestamp, math.max(0, atalaBlockSequenceNumber - blockOffset), b)
          x occurredBefore y must be(false)
      }
    }

    "return true when atalaBlockTimestamp and atalaBlockSequenceNumber are the same and operationSequenceNumber is smaller" in {
      forAll(timeGen, intGen, intGen, intGen) {
        (atalaBlockTimestamp, atalaBlockSequenceNumber, operationSequenceNumber, blockOffset) =>
          val x = TimestampInfo(atalaBlockTimestamp, atalaBlockSequenceNumber, operationSequenceNumber)
          val y = TimestampInfo(atalaBlockTimestamp, atalaBlockSequenceNumber, operationSequenceNumber + blockOffset)
          x occurredBefore y must be(true)
      }
    }

    "return false when atalaBlockTimestamp and atalaBlockSequenceNumber are the same and operationSequenceNumber is larger" in {
      forAll(timeGen, intGen, intGen, intGen) {
        (atalaBlockTimestamp, atalaBlockSequenceNumber, operationSequenceNumber, blockOffset) =>
          val x = TimestampInfo(atalaBlockTimestamp, atalaBlockSequenceNumber, operationSequenceNumber)
          val y = TimestampInfo(
            atalaBlockTimestamp,
            atalaBlockSequenceNumber,
            math.max(0, operationSequenceNumber - blockOffset)
          )
          x occurredBefore y must be(false)
      }
    }

    "return false when everything is the same" in {
      forAll(timeGen, intGen, intGen) { (atalaBlockTimestamp, atalaBlockSequenceNumber, operationSequenceNumber) =>
        val x = TimestampInfo(atalaBlockTimestamp, atalaBlockSequenceNumber, operationSequenceNumber)
        val y = TimestampInfo(atalaBlockTimestamp, atalaBlockSequenceNumber, operationSequenceNumber)
        x occurredBefore y must be(false)
      }
    }
  }
}
