package io.iohk.atala.prism.crypto

import io.iohk.atala.prism.util.BytesOps

case class ECSignature(data: Array[Byte]) {
  def getHexEncoded: String = {
    BytesOps.bytesToHex(data)
  }

  override def toString: String = s"ECSignature(${BytesOps.bytesToHex(data)})"
}
