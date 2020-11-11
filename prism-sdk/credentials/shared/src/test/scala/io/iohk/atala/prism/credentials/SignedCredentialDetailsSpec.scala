package io.iohk.atala.prism.credentials

import io.circe.Json
import io.iohk.atala.prism.identity.DID
import org.scalatest.EitherValues._
import org.scalatest.OptionValues._
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AnyWordSpec

class SignedCredentialDetailsSpec extends AnyWordSpec {

  private val unsignedCredential = JsonBasedUnsignedCredential.jsonBasedUnsignedCredential.buildFrom(
    issuerDID = DID("did:prism:123456678abcdefg"),
    issuanceKeyId = "Issuance-0",
    claims = Json.obj()
  )

  private val signedCredentialString =
    "eyJpc3N1ZXIiOiJkaWQ6cHJpc206MTIzNDU2Njc4YWJjZGVmZyIsImtleUlkIjoiSXNzdWFuY2UtMCIsImNsYWltcyI6e319.MEQCIChNgsIP2fTL65u5uEyjK17QDnn-m08iXzny5ia7CZPNAiBKTKtyvZdAAvGfd8WsSDtKK3ifZnM-hBgk5EHnsw8rrw=="
  private val signedCredential = SignedCredential.from(signedCredentialString).get
  private val slayerCredentialId = "200f5acd3537b819e55b53ea1f87185d512d55e0b48ee37ec649f38b751650db"

  "compute" should {
    "return the proper details" in {
      val result = SignedCredentialDetails.compute(signedCredential.canonicalForm)
      val details = result.toOption.get
      details.credential must be(signedCredential)
      details.issuanceKeyId must be(unsignedCredential.issuanceKeyId.value)
      details.issuerDID must be(unsignedCredential.issuerDID.value)
      details.slayerCredentialId.string must be(slayerCredentialId)
    }

    "fail on invalid input" in {
      val result = SignedCredentialDetails.compute("x" + signedCredential.canonicalForm)
      result.left.value.msg mustNot be(empty)
    }
  }
}
