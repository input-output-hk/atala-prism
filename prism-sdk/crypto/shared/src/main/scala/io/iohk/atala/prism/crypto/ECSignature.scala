package io.iohk.atala.prism.crypto

import io.iohk.atala.prism.util.{BytesOps, BigIntOps}
import io.iohk.atala.prism.util.ArrayOps._
import java.{util => ju}

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

  /**
    * Conversion form ASN.1/DER to P1363
    */
  def toP1363: ECSignature = {
    def extractR(signature: Array[Byte]) = {
      val startR = if ((signature(1) & 0x80) != 0) 3 else 2
      val lengthR = signature(startR + 1)
      BigInt(signature.safeCopyOfRange(startR + 2, startR + 2 + lengthR))
    }

    def extractS(signature: Array[Byte]) = {
      val startR = if ((signature(1) & 0x80) != 0) 3 else 2
      val lengthR = signature(startR + 1)
      val startS = startR + 2 + lengthR
      val lengthS = signature(startS + 1)
      BigInt(signature.safeCopyOfRange(startS + 2, startS + 2 + lengthS))
    }

    ECSignature(
      BigIntOps.toUnsignedByteArray(extractR(data.toArray)) safeAppendedAll
        BigIntOps.toUnsignedByteArray(extractS(data.toArray))
    )
  }

  /**
    * Conversion form P1363 to ASN.1
    */
  def toDer: ECSignature = {
    val size = data.length

    val rawRb = ju.Arrays.copyOfRange(data, 0, size / 2)
    val rawSb = ju.Arrays.copyOfRange(data, size / 2, size)

    // pad values
    val rb = if ((rawRb(0) & 0x80) > 0) rawRb.safePrepended(0x0.toByte) else rawRb
    val sb = if ((rawSb(0) & 0x80) > 0) rawSb.safePrepended(0x0.toByte) else rawSb

    val len = rb.length + sb.length + 4
    val first = Array[Byte](0x30, len.toByte, 0x02, rb.length.toByte)
    val second = Array[Byte](0x02, sb.length.toByte)

    ECSignature(first safeAppendedAll rb safeAppendedAll second safeAppendedAll sb)
  }
}
