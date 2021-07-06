package io.iohk.atala.prism.credentials

import io.iohk.atala.prism.crypto.{AndroidEC, ECTrait}

class AndroidCredentialVerificationSpec extends CredentialVerificationSpecBase {
  override implicit def ec: ECTrait = AndroidEC
}
