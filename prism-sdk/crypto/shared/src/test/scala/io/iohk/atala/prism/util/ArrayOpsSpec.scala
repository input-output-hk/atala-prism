package io.iohk.atala.prism.util

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import io.iohk.atala.prism.util.ArrayOps._

class ArrayOpsSpec extends AnyWordSpec with Matchers {

  "ByteArrayOps" should {
    "prepend element to the array" in new Fixtures {
      arrayOne.safePrepended(0x5) mustBe Array[Byte](0x5, 0x1, 0x2, 0x3)
      empty.safePrepended(0x0) mustBe Array[Byte](0x0)
    }

    "append all to the array" in new Fixtures {
      arrayOne.safeAppendedAll(arrayTwo) mustBe arrayThree
      arrayOne.safeAppendedAll(empty) mustBe Array[Byte](0x1, 0x2, 0x3)
      empty.safeAppendedAll(arrayOne) mustBe Array[Byte](0x1, 0x2, 0x3)
    }

    "copy a rage from the array" in new Fixtures {
      arrayThree.safeCopyOfRange(2, 4) mustBe Array[Byte](0x3, 0x4)
      the[Throwable] thrownBy empty.safeCopyOfRange(2, 4)
    }
  }

  trait Fixtures {
    val arrayOne = Array[Byte](0x1, 0x2, 0x3)
    val arrayTwo = Array[Byte](0x4, 0x5, 0x6)
    val arrayThree = Array[Byte](0x1, 0x2, 0x3, 0x4, 0x5, 0x6)
    val empty = Array[Byte]()
  }
}
