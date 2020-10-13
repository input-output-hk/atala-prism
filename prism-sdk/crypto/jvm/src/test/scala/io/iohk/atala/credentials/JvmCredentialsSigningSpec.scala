package io.iohk.atala.credentials

import io.iohk.atala.crypto.{EC, ECTrait}

class JvmCredentialsSigningSpec extends CredentialsSigningSpecBase {
  override def ec: ECTrait = EC
}
