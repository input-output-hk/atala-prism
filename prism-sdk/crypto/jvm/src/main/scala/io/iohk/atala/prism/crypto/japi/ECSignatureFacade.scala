package io.iohk.atala.prism.crypto.japi

import io.iohk.atala.prism.crypto

class ECSignatureFacade(val signature: crypto.ECSignature) extends ECSignature {
  override def getHexEncoded: String = signature.getHexEncoded

  override def getData: Array[Byte] = signature.data
}
