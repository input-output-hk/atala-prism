package io.iohk.atala.credentials

import java.time.Instant

import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AnyWordSpec

class TimestampInfoSpec extends AnyWordSpec {
  private val before = Instant.now().minusSeconds(1)
  private val now = Instant.now()
  private val after = Instant.now().plusSeconds(1)

  "occurredBefore" should {
    "return true when atalaBlockTimestamp is before" in {
      val isBefore = TimestampInfo(before, 1, 1) occurredBefore TimestampInfo(after, 1, 1)
      isBefore must be(true)
    }

    "return false when atalaBlockTimestamp is later" in {
      val isBefore = TimestampInfo(after, 1, 1) occurredBefore TimestampInfo(before, 1, 1)
      isBefore must be(false)
    }

    "return true when atalaBlockTimestamp is the same and atalaBlockSequenceNumber is smaller" in {
      val isBefore = TimestampInfo(now, 0, 1) occurredBefore TimestampInfo(now, 1, 1)
      isBefore must be(true)
    }

    "return false when atalaBlockTimestamp is the same and atalaBlockSequenceNumber is larger" in {
      val isBefore = TimestampInfo(now, 2, 1) occurredBefore TimestampInfo(now, 1, 1)
      isBefore must be(false)
    }

    "return true when atalaBlockTimestamp and atalaBlockSequenceNumber are the same and operationSequenceNumber is smaller" in {
      val isBefore = TimestampInfo(now, 1, 0) occurredBefore TimestampInfo(now, 1, 1)
      isBefore must be(true)
    }

    "return false when atalaBlockTimestamp and atalaBlockSequenceNumber are the same and operationSequenceNumber is larger" in {
      val isBefore = TimestampInfo(now, 1, 2) occurredBefore TimestampInfo(now, 1, 1)
      isBefore must be(false)
    }

    "return false when everything is the same" in {
      val isBefore = TimestampInfo(now, 1, 1) occurredBefore TimestampInfo(now, 1, 1)
      isBefore must be(false)
    }
  }
}
