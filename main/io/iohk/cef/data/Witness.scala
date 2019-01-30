package io.iohk.cef.data
import io.iohk.cef.crypto._

case class Witness(key: SigningPublicKey, signature: Signature)
