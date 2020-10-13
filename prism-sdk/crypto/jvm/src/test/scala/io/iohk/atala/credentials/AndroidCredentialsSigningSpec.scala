package io.iohk.atala.credentials

import io.iohk.atala.crypto.{AndroidEC, ECTrait}

class AndroidCredentialsSigningSpec extends CredentialsSigningSpecBase {
  override def ec: ECTrait = AndroidEC
}
