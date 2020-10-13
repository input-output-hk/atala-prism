package io.iohk.atala.credentials

import io.iohk.atala.crypto.{EC, ECTrait}

class JsCredentialsSigningSpec extends CredentialsSigningSpecBase {
  override def ec: ECTrait = EC
}
