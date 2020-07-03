package io.iohk.atala.crypto

case class ECSignature(data: Array[Byte]) {
  def getHexEncoded: String = {
    ECUtils.bytesToHex(data)
  }
}
