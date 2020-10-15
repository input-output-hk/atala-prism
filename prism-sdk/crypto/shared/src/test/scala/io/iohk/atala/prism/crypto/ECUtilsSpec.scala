package io.iohk.atala.prism.crypto

import org.scalacheck.Gen
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks._

class ECUtilsSpec extends AnyWordSpec {

  val hexChars = "0123456789abcdef"
  val hexStringGen: Gen[String] =
    Gen.containerOf[Seq, Char](Gen.oneOf(hexChars)).withFilter(_.size % 2 == 0).map(_.mkString(""))

  "hexToBytes" should {
    "decode bytes encoded by bytesToHex" in {
      forAll { bytes: Array[Byte] =>
        val decodedBytes = ECUtils.hexToBytes(ECUtils.bytesToHex(bytes))
        decodedBytes must contain theSameElementsInOrderAs bytes
      }
    }
  }

  "bytesToHex" should {
    "encode bytes decoded by hexToBytes" in {
      forAll(hexStringGen) { hexString =>
        val encodedString = ECUtils.bytesToHex(ECUtils.hexToBytes(hexString))
        encodedString mustBe hexString
      }
    }
  }

}
