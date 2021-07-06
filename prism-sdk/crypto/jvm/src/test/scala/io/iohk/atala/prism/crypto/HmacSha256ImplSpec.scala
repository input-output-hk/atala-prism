package io.iohk.atala.prism.crypto

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers

import io.iohk.atala.prism.util.BytesOps

class HmacSha256ImplSpec extends AnyWordSpec with Matchers {

  "HmacSha256Impl" should {

    "compute HMAC-SHA256" in {
      BytesOps.bytesToHex(
        HmacSha256Impl.compute("test".getBytes, "secret".getBytes)
      ) mustBe "0329a06b62cd16b33eb6792be8c60b158d89a2ee3a876fce9a881ebb488c0914"
    }

    "compute HMAC-SHA256 from EC private key" in {
      val keyPair = EC.generateKeyPair()
      val hmac = HmacSha256Impl.compute(keyPair.privateKey.getEncoded, "secret".getBytes)

      hmac.size mustBe 32 // SHA-256 uses 32-bytes secret

      // should be deterministic
      hmac mustBe HmacSha256Impl.compute(keyPair.privateKey.getEncoded, "secret".getBytes)
    }
  }

}
