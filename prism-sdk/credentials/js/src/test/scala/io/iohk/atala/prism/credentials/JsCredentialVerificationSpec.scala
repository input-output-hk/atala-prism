package io.iohk.atala.prism.credentials
import io.iohk.atala.prism.crypto.{EC, ECTrait}

class JsCredentialVerificationSpec extends CredentialVerificationSpecBase {
  override implicit def ec: ECTrait = EC
}
