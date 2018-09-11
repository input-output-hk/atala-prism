package io.iohk.cef.crypto.low

import akka.util.ByteString
import org.bouncycastle.util.encoders.Hex
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.prop.PropertyChecks

class HashingAlogorithmSpec extends FlatSpec with PropertyChecks {

  "sha256" should "get the correct result for a ByteString" in {
    hashBytes(HashAlgorithm.SHA256)(ByteString("a")) shouldBe Hex.decode(
      "ca978112ca1bbdcafac231b39a23dc4da786eff8147c4e72b9807785afee48bb")
  }

  "sha256" should "generate hashes that are unique for each ByteString" in {

    val algo = HashAlgorithm.SHA256

    forAll { (a: String, b: String) =>
      whenever(a != b) {
        hashBytes(algo)(ByteString(a)) should !==(hashBytes(algo)(ByteString(b)))
      }
    }
  }

}
