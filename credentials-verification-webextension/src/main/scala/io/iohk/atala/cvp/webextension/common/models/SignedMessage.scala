package io.iohk.atala.cvp.webextension.common.models

case class SignedMessage(did: String, didKeyId: String, signature: Array[Byte])
