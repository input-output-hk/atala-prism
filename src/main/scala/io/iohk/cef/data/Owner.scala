package io.iohk.cef.data

import io.iohk.cef.crypto._

case class Owner(key: SigningPublicKey, signature: Signature)
