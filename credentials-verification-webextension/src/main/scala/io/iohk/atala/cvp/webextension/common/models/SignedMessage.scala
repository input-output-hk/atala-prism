package io.iohk.atala.cvp.webextension.common.models

case class SignedMessage(did: String, didKeyId: String, base64UrlSignature: String, base64UrlNonce: String)
