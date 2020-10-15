package io.iohk.atala.prism.crypto

case class ECSignature(data: Array[Byte]) {
  def getHexEncoded: String = {
    ECUtils.bytesToHex(data)
  }
}
