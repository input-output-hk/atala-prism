package io.iohk.atala.prism.credentials

import io.iohk.atala.prism.crypto.{AndroidEC, ECTrait}

class AndroidCredentialsSigningSpec extends CredentialsSigningSpecBase {
  override def ec: ECTrait = AndroidEC
}
