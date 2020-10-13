package io.iohk.atala.credentials
import io.iohk.atala.crypto.{EC, ECTrait}

class JsCredentialVerificationSpec extends CredentialVerificationSpecBase {
  override implicit def ec: ECTrait = EC
}
