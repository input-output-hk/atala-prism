package io.iohk.atala.prism.node.operations

import java.time.Instant

import org.scalatest.{MustMatchers, WordSpec}

class TimestampInfoSpec extends WordSpec with MustMatchers {
  "TimestampInfo.occurredBefore" should {

    "compare correctly 1" in {
      val a = TimestampInfo(Instant.ofEpochMilli(0), 0, 0)
      val b = TimestampInfo(Instant.ofEpochMilli(0), 0, 0)
      a occurredBefore b mustBe false
    }

    "compare correctly 2" in {
      val a = TimestampInfo(Instant.ofEpochMilli(0), 0, 0)
      val b = TimestampInfo(Instant.ofEpochMilli(0), 0, 1)
      a occurredBefore b mustBe true
    }

    "compare correctly 3" in {
      val a = TimestampInfo(Instant.ofEpochMilli(0), 0, 0)
      val b = TimestampInfo(Instant.ofEpochMilli(0), 1, 0)
      a occurredBefore b mustBe true
    }

    "compare correctly 4" in {
      val a = TimestampInfo(Instant.ofEpochMilli(0), 0, 0)
      val b = TimestampInfo(Instant.ofEpochMilli(0), 1, 1)
      a occurredBefore b mustBe true
    }

    "compare correctly 5" in {
      val a = TimestampInfo(Instant.ofEpochMilli(0), 0, 0)
      val b = TimestampInfo(Instant.ofEpochMilli(1), 0, 0)
      a occurredBefore b mustBe true
    }

    "compare correctly 6" in {
      val a = TimestampInfo(Instant.ofEpochMilli(0), 0, 0)
      val b = TimestampInfo(Instant.ofEpochMilli(1), 0, 1)
      a occurredBefore b mustBe true
    }

    "compare correctly 7" in {
      val a = TimestampInfo(Instant.ofEpochMilli(0), 0, 0)
      val b = TimestampInfo(Instant.ofEpochMilli(1), 1, 0)
      a occurredBefore b mustBe true
    }

    "compare correctly 8" in {
      val a = TimestampInfo(Instant.ofEpochMilli(0), 0, 0)
      val b = TimestampInfo(Instant.ofEpochMilli(1), 1, 1)
      a occurredBefore b mustBe true
    }

    "compare correctly 9" in {
      val a = TimestampInfo(Instant.ofEpochMilli(0), 0, 1)
      val b = TimestampInfo(Instant.ofEpochMilli(0), 0, 0)
      a occurredBefore b mustBe false
    }

    "compare correctly 10" in {
      val a = TimestampInfo(Instant.ofEpochMilli(0), 0, 1)
      val b = TimestampInfo(Instant.ofEpochMilli(0), 0, 1)
      a occurredBefore b mustBe false
    }

    "compare correctly 11" in {
      val a = TimestampInfo(Instant.ofEpochMilli(0), 0, 1)
      val b = TimestampInfo(Instant.ofEpochMilli(0), 1, 0)
      a occurredBefore b mustBe true
    }

    "compare correctly 12" in {
      val a = TimestampInfo(Instant.ofEpochMilli(0), 0, 1)
      val b = TimestampInfo(Instant.ofEpochMilli(0), 1, 1)
      a occurredBefore b mustBe true
    }

    "compare correctly 13" in {
      val a = TimestampInfo(Instant.ofEpochMilli(0), 0, 1)
      val b = TimestampInfo(Instant.ofEpochMilli(1), 0, 0)
      a occurredBefore b mustBe true
    }

    "compare correctly 14" in {
      val a = TimestampInfo(Instant.ofEpochMilli(0), 0, 1)
      val b = TimestampInfo(Instant.ofEpochMilli(1), 0, 1)
      a occurredBefore b mustBe true
    }

    "compare correctly 15" in {
      val a = TimestampInfo(Instant.ofEpochMilli(0), 0, 1)
      val b = TimestampInfo(Instant.ofEpochMilli(1), 1, 0)
      a occurredBefore b mustBe true
    }

    "compare correctly 16" in {
      val a = TimestampInfo(Instant.ofEpochMilli(0), 0, 1)
      val b = TimestampInfo(Instant.ofEpochMilli(1), 1, 1)
      a occurredBefore b mustBe true
    }

    "compare correctly 17" in {
      val a = TimestampInfo(Instant.ofEpochMilli(0), 1, 0)
      val b = TimestampInfo(Instant.ofEpochMilli(0), 0, 0)
      a occurredBefore b mustBe false
    }

    "compare correctly 18" in {
      val a = TimestampInfo(Instant.ofEpochMilli(0), 1, 0)
      val b = TimestampInfo(Instant.ofEpochMilli(0), 0, 1)
      a occurredBefore b mustBe false
    }

    "compare correctly 19" in {
      val a = TimestampInfo(Instant.ofEpochMilli(0), 1, 0)
      val b = TimestampInfo(Instant.ofEpochMilli(0), 1, 0)
      a occurredBefore b mustBe false
    }

    "compare correctly 20" in {
      val a = TimestampInfo(Instant.ofEpochMilli(0), 1, 0)
      val b = TimestampInfo(Instant.ofEpochMilli(0), 1, 1)
      a occurredBefore b mustBe true
    }

    "compare correctly 21" in {
      val a = TimestampInfo(Instant.ofEpochMilli(0), 1, 0)
      val b = TimestampInfo(Instant.ofEpochMilli(1), 0, 0)
      a occurredBefore b mustBe true
    }

    "compare correctly 22" in {
      val a = TimestampInfo(Instant.ofEpochMilli(0), 1, 0)
      val b = TimestampInfo(Instant.ofEpochMilli(1), 0, 1)
      a occurredBefore b mustBe true
    }

    "compare correctly 23" in {
      val a = TimestampInfo(Instant.ofEpochMilli(0), 1, 0)
      val b = TimestampInfo(Instant.ofEpochMilli(1), 1, 0)
      a occurredBefore b mustBe true
    }

    "compare correctly 24" in {
      val a = TimestampInfo(Instant.ofEpochMilli(0), 1, 0)
      val b = TimestampInfo(Instant.ofEpochMilli(1), 1, 1)
      a occurredBefore b mustBe true
    }

    "compare correctly 25" in {
      val a = TimestampInfo(Instant.ofEpochMilli(0), 1, 1)
      val b = TimestampInfo(Instant.ofEpochMilli(0), 0, 0)
      a occurredBefore b mustBe false
    }

    "compare correctly 26" in {
      val a = TimestampInfo(Instant.ofEpochMilli(0), 1, 1)
      val b = TimestampInfo(Instant.ofEpochMilli(0), 0, 1)
      a occurredBefore b mustBe false
    }

    "compare correctly 27" in {
      val a = TimestampInfo(Instant.ofEpochMilli(0), 1, 1)
      val b = TimestampInfo(Instant.ofEpochMilli(0), 1, 0)
      a occurredBefore b mustBe false
    }

    "compare correctly 28" in {
      val a = TimestampInfo(Instant.ofEpochMilli(0), 1, 1)
      val b = TimestampInfo(Instant.ofEpochMilli(0), 1, 1)
      a occurredBefore b mustBe false
    }

    "compare correctly 29" in {
      val a = TimestampInfo(Instant.ofEpochMilli(0), 1, 1)
      val b = TimestampInfo(Instant.ofEpochMilli(1), 0, 0)
      a occurredBefore b mustBe true
    }

    "compare correctly 30" in {
      val a = TimestampInfo(Instant.ofEpochMilli(0), 1, 1)
      val b = TimestampInfo(Instant.ofEpochMilli(1), 0, 1)
      a occurredBefore b mustBe true
    }

    "compare correctly 31" in {
      val a = TimestampInfo(Instant.ofEpochMilli(0), 1, 1)
      val b = TimestampInfo(Instant.ofEpochMilli(1), 1, 0)
      a occurredBefore b mustBe true
    }

    "compare correctly 32" in {
      val a = TimestampInfo(Instant.ofEpochMilli(0), 1, 1)
      val b = TimestampInfo(Instant.ofEpochMilli(1), 1, 1)
      a occurredBefore b mustBe true
    }

    "compare correctly 33" in {
      val a = TimestampInfo(Instant.ofEpochMilli(1), 0, 0)
      val b = TimestampInfo(Instant.ofEpochMilli(0), 0, 0)
      a occurredBefore b mustBe false
    }

    "compare correctly 34" in {
      val a = TimestampInfo(Instant.ofEpochMilli(1), 0, 0)
      val b = TimestampInfo(Instant.ofEpochMilli(0), 0, 1)
      a occurredBefore b mustBe false
    }

    "compare correctly 35" in {
      val a = TimestampInfo(Instant.ofEpochMilli(1), 0, 0)
      val b = TimestampInfo(Instant.ofEpochMilli(0), 1, 0)
      a occurredBefore b mustBe false
    }

    "compare correctly 36" in {
      val a = TimestampInfo(Instant.ofEpochMilli(1), 0, 0)
      val b = TimestampInfo(Instant.ofEpochMilli(0), 1, 1)
      a occurredBefore b mustBe false
    }

    "compare correctly 37" in {
      val a = TimestampInfo(Instant.ofEpochMilli(1), 0, 0)
      val b = TimestampInfo(Instant.ofEpochMilli(1), 0, 0)
      a occurredBefore b mustBe false
    }

    "compare correctly 38" in {
      val a = TimestampInfo(Instant.ofEpochMilli(1), 0, 0)
      val b = TimestampInfo(Instant.ofEpochMilli(1), 0, 1)
      a occurredBefore b mustBe true
    }

    "compare correctly 39" in {
      val a = TimestampInfo(Instant.ofEpochMilli(1), 0, 0)
      val b = TimestampInfo(Instant.ofEpochMilli(1), 1, 0)
      a occurredBefore b mustBe true
    }

    "compare correctly 40" in {
      val a = TimestampInfo(Instant.ofEpochMilli(1), 0, 0)
      val b = TimestampInfo(Instant.ofEpochMilli(1), 1, 1)
      a occurredBefore b mustBe true
    }

    "compare correctly 41" in {
      val a = TimestampInfo(Instant.ofEpochMilli(1), 0, 1)
      val b = TimestampInfo(Instant.ofEpochMilli(0), 0, 0)
      a occurredBefore b mustBe false
    }

    "compare correctly 42" in {
      val a = TimestampInfo(Instant.ofEpochMilli(1), 0, 1)
      val b = TimestampInfo(Instant.ofEpochMilli(0), 0, 1)
      a occurredBefore b mustBe false
    }

    "compare correctly 43" in {
      val a = TimestampInfo(Instant.ofEpochMilli(1), 0, 1)
      val b = TimestampInfo(Instant.ofEpochMilli(0), 1, 0)
      a occurredBefore b mustBe false
    }

    "compare correctly 44" in {
      val a = TimestampInfo(Instant.ofEpochMilli(1), 0, 1)
      val b = TimestampInfo(Instant.ofEpochMilli(0), 1, 1)
      a occurredBefore b mustBe false
    }

    "compare correctly 45" in {
      val a = TimestampInfo(Instant.ofEpochMilli(1), 0, 1)
      val b = TimestampInfo(Instant.ofEpochMilli(1), 0, 0)
      a occurredBefore b mustBe false
    }

    "compare correctly 46" in {
      val a = TimestampInfo(Instant.ofEpochMilli(1), 0, 1)
      val b = TimestampInfo(Instant.ofEpochMilli(1), 0, 1)
      a occurredBefore b mustBe false
    }

    "compare correctly 47" in {
      val a = TimestampInfo(Instant.ofEpochMilli(1), 0, 1)
      val b = TimestampInfo(Instant.ofEpochMilli(1), 1, 0)
      a occurredBefore b mustBe true
    }

    "compare correctly 48" in {
      val a = TimestampInfo(Instant.ofEpochMilli(1), 0, 1)
      val b = TimestampInfo(Instant.ofEpochMilli(1), 1, 1)
      a occurredBefore b mustBe true
    }

    "compare correctly 49" in {
      val a = TimestampInfo(Instant.ofEpochMilli(1), 1, 0)
      val b = TimestampInfo(Instant.ofEpochMilli(0), 0, 0)
      a occurredBefore b mustBe false
    }

    "compare correctly 50" in {
      val a = TimestampInfo(Instant.ofEpochMilli(1), 1, 0)
      val b = TimestampInfo(Instant.ofEpochMilli(0), 0, 1)
      a occurredBefore b mustBe false
    }

    "compare correctly 51" in {
      val a = TimestampInfo(Instant.ofEpochMilli(1), 1, 0)
      val b = TimestampInfo(Instant.ofEpochMilli(0), 1, 0)
      a occurredBefore b mustBe false
    }

    "compare correctly 52" in {
      val a = TimestampInfo(Instant.ofEpochMilli(1), 1, 0)
      val b = TimestampInfo(Instant.ofEpochMilli(0), 1, 1)
      a occurredBefore b mustBe false
    }

    "compare correctly 53" in {
      val a = TimestampInfo(Instant.ofEpochMilli(1), 1, 0)
      val b = TimestampInfo(Instant.ofEpochMilli(1), 0, 0)
      a occurredBefore b mustBe false
    }

    "compare correctly 54" in {
      val a = TimestampInfo(Instant.ofEpochMilli(1), 1, 0)
      val b = TimestampInfo(Instant.ofEpochMilli(1), 0, 1)
      a occurredBefore b mustBe false
    }

    "compare correctly 55" in {
      val a = TimestampInfo(Instant.ofEpochMilli(1), 1, 0)
      val b = TimestampInfo(Instant.ofEpochMilli(1), 1, 0)
      a occurredBefore b mustBe false
    }

    "compare correctly 56" in {
      val a = TimestampInfo(Instant.ofEpochMilli(1), 1, 0)
      val b = TimestampInfo(Instant.ofEpochMilli(1), 1, 1)
      a occurredBefore b mustBe true
    }

    "compare correctly 57" in {
      val a = TimestampInfo(Instant.ofEpochMilli(1), 1, 1)
      val b = TimestampInfo(Instant.ofEpochMilli(0), 0, 0)
      a occurredBefore b mustBe false
    }

    "compare correctly 58" in {
      val a = TimestampInfo(Instant.ofEpochMilli(1), 1, 1)
      val b = TimestampInfo(Instant.ofEpochMilli(0), 0, 1)
      a occurredBefore b mustBe false
    }

    "compare correctly 59" in {
      val a = TimestampInfo(Instant.ofEpochMilli(1), 1, 1)
      val b = TimestampInfo(Instant.ofEpochMilli(0), 1, 0)
      a occurredBefore b mustBe false
    }

    "compare correctly 60" in {
      val a = TimestampInfo(Instant.ofEpochMilli(1), 1, 1)
      val b = TimestampInfo(Instant.ofEpochMilli(0), 1, 1)
      a occurredBefore b mustBe false
    }

    "compare correctly 61" in {
      val a = TimestampInfo(Instant.ofEpochMilli(1), 1, 1)
      val b = TimestampInfo(Instant.ofEpochMilli(1), 0, 0)
      a occurredBefore b mustBe false
    }

    "compare correctly 62" in {
      val a = TimestampInfo(Instant.ofEpochMilli(1), 1, 1)
      val b = TimestampInfo(Instant.ofEpochMilli(1), 0, 1)
      a occurredBefore b mustBe false
    }

    "compare correctly 63" in {
      val a = TimestampInfo(Instant.ofEpochMilli(1), 1, 1)
      val b = TimestampInfo(Instant.ofEpochMilli(1), 1, 0)
      a occurredBefore b mustBe false
    }

    "compare correctly 64" in {
      val a = TimestampInfo(Instant.ofEpochMilli(1), 1, 1)
      val b = TimestampInfo(Instant.ofEpochMilli(1), 1, 1)
      a occurredBefore b mustBe false
    }

  }
}
