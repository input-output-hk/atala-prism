package io.iohk.atala.prism.crypto

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.util.Try

class SHA256ConstructorsSpec extends AnyWordSpec with Matchers {
  "SHA256 constructors methods" should {
    "throw expected errors in unsafe constructors" in {
      val emptyString = ""
      val maybeEmptyStringError = Try(Sha256Digest.fromHexUnsafe(emptyString))

      maybeEmptyStringError.isFailure mustBe true
      maybeEmptyStringError.failed.get.getMessage mustBe """The input string "" doesn't match regexp - "^(?:[0-9a-fA-F]{2})+$""""

      val invalidString = "12345"
      val maybeInvalidStringError = Try(Sha256Digest.fromHexUnsafe(invalidString))
      maybeInvalidStringError.isFailure mustBe true
      maybeInvalidStringError.failed.get.getMessage mustBe """The input string "12345" doesn't match regexp - "^(?:[0-9a-fA-F]{2})+$""""

      val invalidVector = Vector(0x84.toByte)
      val maybeInvalidVectorError = Try(Sha256Digest.fromVectorUnsafe(invalidVector))
      maybeInvalidVectorError.isFailure mustBe true
      maybeInvalidVectorError.failed.get.getMessage mustBe "Vector length doesn't correspond to expected length  - 32"
    }
  }
}
