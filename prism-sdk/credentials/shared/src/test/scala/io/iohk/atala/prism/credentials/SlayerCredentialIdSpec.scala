package io.iohk.atala.prism.credentials

import io.iohk.atala.prism.credentials.Credential
import io.iohk.atala.prism.identity.DID
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AnyWordSpec

class SlayerCredentialIdSpec extends AnyWordSpec {
  "compute" should {
    "compute the proper id" in {
      val did = DID.buildPrismDID("123456678abcdefg")
      val signedCredentialString =
        "eyJ0eXBlIjpbIlZlcmlmaWFibGVDcmVkZW50aWFsIiwiUmVkbGFuZElkQ3JlZGVudGlhbCJdLCJpc3N1ZXIiOiJkaWQ6cHJpc206MTIzNDU2Njc4YWJjZGVmZyIsImtleUlkIjoiSXNzdWFuY2UtMCJ9.MEUCICmZ463ZZbwNbAuA8TuHFkO0PM0H1UfZtdk2V7YLKFVIAiEAuKELUaOFd75N753Bt2qeNm7ah5fPtvQhgbYzpwB2_Ow="
      val signedCredential = Credential.fromString(signedCredentialString).toOption.get
      val expected = "4d1c2f99aeb0a211eb4889ae9ef29368b763c2bd00ad1206f294f9b20f288983"

      val actual = SlayerCredentialId.compute(signedCredential, did).string
      actual must be(expected)
    }
  }
}
