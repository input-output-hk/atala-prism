package io.iohk.atala.prism.credentials

import io.iohk.atala.prism.credentials.Credential
import io.iohk.atala.prism.identity.DID
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.OptionValues._

class SlayerCredentialIdSpec extends AnyWordSpec {
  "compute" should {
    "compute the proper id" in {
      val did = DID.buildPrismDID("123456678abcdefg")
      val signedCredentialString =
        "eyJ0eXBlIjpbIlZlcmlmaWFibGVDcmVkZW50aWFsIiwiUmVkbGFuZElkQ3JlZGVudGlhbCJdLCJpZCI6ImRpZDpwcmlzbToxMjM0NTY2NzhhYmNkZWZnIiwia2V5SWQiOiJJc3N1YW5jZS0wIn0.MEUCICmZ463ZZbwNbAuA8TuHFkO0PM0H1UfZtdk2V7YLKFVIAiEAuKELUaOFd75N753Bt2qeNm7ah5fPtvQhgbYzpwB2_Ow="
      val signedCredential = Credential.fromString(signedCredentialString).toOption.get
      val expected = "cd9d883aa47727b81c7eccb39e44c9ec640b8b8dc87295a757bf280230ed0c8d"

      val actual = SlayerCredentialId.compute(signedCredential, did).string
      actual must be(expected)
    }

    "compute proper id from an encoded signed credential" in {

      // this is the encoded JSON
      // {"type":["VerifiableCredential","RedlandIdCredential"],"id":"did:prism:123456678abcdefg","keyId":"Issuance-0"}
      // and we use a random base64URL as "signature" after the "." (we do not mind about the signature)
      val signedCredentialString =
        "eyJ0eXBlIjpbIlZlcmlmaWFibGVDcmVkZW50aWFsIiwiUmVkbGFuZElkQ3JlZGVudGlhbCJdLCJpZCI6ImRpZDpwcmlzbToxMjM0NTY2NzhhYmNkZWZnIiwia2V5SWQiOiJJc3N1YW5jZS0wIn0.MEUCICmZ463ZZbwNbAuA8TuHFkO0PM0H1UfZtdk2V7YLKFVIAiEAuKELUaOFd75N753Bt2qeNm7ah5fPtvQhgbYzpwB2_Ow="
      val expected = "cd9d883aa47727b81c7eccb39e44c9ec640b8b8dc87295a757bf280230ed0c8d"

      val actual = SlayerCredentialId.compute(signedCredentialString).toOption.value.string

      actual must be(expected)
    }
  }
}
