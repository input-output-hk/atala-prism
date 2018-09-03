package io.iohk.cef.crypto.low

import akka.util.ByteString
import org.bouncycastle.util.encoders.Hex
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.prop.PropertyChecks

class HashingAlogorithmSpec extends FlatSpec with PropertyChecks {

  "kec256" should "get the correct result for a ByteString" in {
    hashBytes(HashAlgorithm.KEC256)(ByteString("a")) shouldBe Hex.decode(
      "3ac225168df54212a25c1c01fd35bebfea408fdac2e31ddd6f80a4bbf9a5f1cb")
  }

  "kec256" should "generate hashes that are unique for each ByteString" in {

    val algo = HashAlgorithm.KEC256

    forAll { (a: String, b: String) =>
      whenever(a != b) {
        hashBytes(algo)(ByteString(a)) should !==(hashBytes(algo)(ByteString(b)))
      }
    }
  }

}
