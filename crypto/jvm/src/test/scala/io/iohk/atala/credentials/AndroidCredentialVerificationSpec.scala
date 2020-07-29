package io.iohk.atala.credentials

import io.iohk.atala.crypto.{AndroidEC, ECTrait}

class AndroidCredentialVerificationSpec extends CredentialVerificationSpecBase {
  override implicit def ec: ECTrait = AndroidEC
}
