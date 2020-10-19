package io.iohk.atala.prism.credentials

import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AnyWordSpec

class SlayerCredentialIdSpec extends AnyWordSpec {
  "compute" should {
    "compute the proper id" in {
      val did = "did:prism:123456678abcdefg"
      val signedCredentialString =
        "eyJpc3N1ZXIiOiJkaWQ6cHJpc206MTIzNDU2Njc4YWJjZGVmZyIsImtleUlkIjoiSXNzdWFuY2UtMCIsImNsYWltcyI6e319.MEQCIChNgsIP2fTL65u5uEyjK17QDnn-m08iXzny5ia7CZPNAiBKTKtyvZdAAvGfd8WsSDtKK3ifZnM-hBgk5EHnsw8rrw=="
      val signedCredential = SignedCredential.from(signedCredentialString).get
      val expected = "200f5acd3537b819e55b53ea1f87185d512d55e0b48ee37ec649f38b751650db"

      val actual = SlayerCredentialId.compute(signedCredential, did).string
      actual must be(expected)
    }
  }
}
