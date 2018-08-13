package io.iohk.cef.crypto.low

import akka.util.ByteString
import org.bouncycastle.util.encoders.Hex
import org.scalatest.FlatSpec
import org.scalatest.Matchers._

class HashingAlogorithmSpec extends FlatSpec {

  "kec256" should "get the correct result for a ByteString" in {
    ByteString("a").hashWith(HashAlgorithm.KEC256) shouldBe Hex.decode("3ac225168df54212a25c1c01fd35bebfea408fdac2e31ddd6f80a4bbf9a5f1cb")
  }

}
