package io.iohk.atala.prism.utils

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class Base64UtilSpec extends AnyWordSpec with Matchers {

  "encodeURl" should {
    "encode byte array into a base64URL string" in {
      val bytes = "test".getBytes
      val encodedString = "dGVzdA=="
      Base64Utils.encodeURL(bytes) mustBe encodedString
    }
  }

  "decodeURL" should {
    "decode base64URL string into a byte array" in {
      val originalString = "test"
      val encodedString = "dGVzdA=="
      val byteArray = originalString.getBytes
      Base64Utils.decodeURL(encodedString) mustBe byteArray
    }
  }

  "decodeUrlToString" should {
    "decode base64URL string into a string" in {
      val encodedString = "dGVzdA=="
      val originalString = "test"
      Base64Utils.decodeUrlToString(encodedString) mustBe originalString
    }
  }

}
