package io.iohk.atala.prism.crypto

case class ECSignature(data: Array[Byte]) {
  def getHexEncoded: String = {
    ECUtils.bytesToHex(data)
  }

  override def toString: String = s"ECSignature(${ECUtils.bytesToHex(data)})"
}
