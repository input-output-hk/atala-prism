package io.iohk.cef

import org.bouncycastle.crypto.params.ECPublicKeyParameters

package object network {
  implicit class ECPublicKeyParametersNodeId(val pubKey: ECPublicKeyParameters) extends AnyVal {
    def toNodeId: Array[Byte] =
      pubKey.asInstanceOf[ECPublicKeyParameters].getQ
        .getEncoded(false)
        .drop(1) // drop type info
  }
}
