package io.iohk.atala.prism.credentials

import io.circe.Json
import io.iohk.atala.prism.identity.DID
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.OptionValues._

abstract class UnsignedCredentialSpec[A: UnsignedCredentialBuilder] extends AnyWordSpec {

  private val builder = UnsignedCredentialBuilder[A]

  private val mockIssuerDID = DID("did:prism:123456678abcdefg")
  private val mockIssuanceKeyid = "Issuance-0"
  private val mockClaims = Json.obj()

  private val mockUnsignedCredential = builder.buildFrom(mockIssuerDID, mockIssuanceKeyid, mockClaims)

  private val emptyCredential = builder.fromBytes(Json.obj().noSpaces.getBytes(charsetUsed))

  "UnsignedCredential" should {
    "reconstruct the original credential after parsing" in {
      val credentialBytes = mockUnsignedCredential.bytes

      mockUnsignedCredential must be(builder.fromBytes(credentialBytes))
    }

    "fail to construct when bytes are not from a valid JSON" in {
      val bytesThatAreNotAJson = "Not A JSON".getBytes(charsetUsed)

      intercept[Exception](
        builder.fromBytes(bytesThatAreNotAJson)
      )
    }

    "return the correct issuer DID when present" in {
      mockUnsignedCredential.issuerDID.value must be(mockIssuerDID)
    }

    "return empty when the issuer DID is not present" in {
      emptyCredential.issuerDID must be(empty)
    }

    "return the correct issuance key id" in {
      mockUnsignedCredential.issuanceKeyId.value must be(mockIssuanceKeyid)
    }

    "return empty when the issuance key id is not present" in {
      emptyCredential.issuanceKeyId must be(empty)
    }
  }
}
