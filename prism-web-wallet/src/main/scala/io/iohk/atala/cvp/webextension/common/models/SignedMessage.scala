package io.iohk.atala.cvp.webextension.common.models

import typings.inputOutputHkPrismSdk.mod.io.iohk.atala.prism.kotlin.identity.DID

case class SignedMessage(did: DID, didKeyId: String, base64UrlSignature: String, base64UrlNonce: String)
