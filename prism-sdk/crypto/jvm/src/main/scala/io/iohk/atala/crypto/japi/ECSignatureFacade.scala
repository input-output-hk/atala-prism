package io.iohk.atala.crypto.japi

import io.iohk.atala.crypto

class ECSignatureFacade(val signature: crypto.ECSignature) extends ECSignature {
  override def getHexEncoded: String = signature.getHexEncoded

  override def getData: Array[Byte] = signature.data
}
