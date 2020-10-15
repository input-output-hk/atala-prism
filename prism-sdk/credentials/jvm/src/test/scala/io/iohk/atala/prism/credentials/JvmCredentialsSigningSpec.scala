package io.iohk.atala.prism.credentials

import io.iohk.atala.prism.crypto.{EC, ECTrait}

class JvmCredentialsSigningSpec extends CredentialsSigningSpecBase {
  override def ec: ECTrait = EC
}
