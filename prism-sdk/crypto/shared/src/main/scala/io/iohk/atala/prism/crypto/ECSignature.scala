package io.iohk.atala.prism.crypto

import io.iohk.atala.prism.util.BytesOps

case class ECSignature(data: Array[Byte]) {
  def getHexEncoded: String = {
    BytesOps.bytesToHex(data)
  }

  override def toString: String = s"ECSignature($getHexEncoded)"

  /**
    * Allow comparing [[ECSignature]] by converting data to [[IndexedSeq]].
    */
  override def equals(arg: Any) = {
    arg match {
      case ECSignature(d) => data.toIndexedSeq == d.toIndexedSeq
      case _ => false
    }
  }

  override def hashCode = data.toIndexedSeq.hashCode
}
