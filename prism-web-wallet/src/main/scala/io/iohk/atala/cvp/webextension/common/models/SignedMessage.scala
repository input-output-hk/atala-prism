package io.iohk.atala.cvp.webextension.common.models

import io.iohk.atala.prism.identity.DID

case class SignedMessage(did: DID, didKeyId: String, base64UrlSignature: String, base64UrlNonce: String)
