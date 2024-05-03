package io.iohk.atala.prism.node.utils

import org.scalacheck.Gen
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks._

class BytesOpsSpec extends AnyWordSpec {
  val hexChars = "0123456789abcdef"
  val hexStringGen: Gen[String] =
    Gen
      .containerOf[Seq, Char](Gen.oneOf(hexChars))
      .withFilter(_.size % 2 == 0)
      .map(_.mkString(""))

  "hexToBytes" should {
    "decode bytes encoded by bytesToHex" in {
      forAll { bytes: Array[Byte] =>
        val decodedBytes = BytesOps.hexToBytes(BytesOps.bytesToHex(bytes))
        decodedBytes must contain theSameElementsInOrderAs bytes
      }
    }
  }

  "bytesToHex" should {
    "encode bytes decoded by hexToBytes" in {
      forAll(hexStringGen) { hexString =>
        val encodedString = BytesOps.bytesToHex(BytesOps.hexToBytes(hexString))
        encodedString mustBe hexString
      }
    }
  }
}
