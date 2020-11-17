package io.iohk.atala.prism.crypto

import io.iohk.atala.prism.util.{BytesOps, BigIntOps}
import io.iohk.atala.prism.util.ArrayOps._

case class ECSignature(data: Array[Byte]) {
  def getHexEncoded: String = {
    BytesOps.bytesToHex(data)
  }

  override def toString: String = s"ECSignature($getHexEncoded)"

  /**
    * Allow comparing [[ECSignature]] by converting data to [[scala.IndexedSeq]].
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
    *
    * P1363 contains two integer wothout separator, ASN.1 signature format looks like:
    *
    * {{{
    *   ECDSASignature ::= SEQUENCE {
    *     r INTEGER,
    *     s INTEGER
    *   }
    * }}}
    *
    * The solution is taken from: https://github.com/google/wycheproof/blob/e91db8abc22df105a2bce5be452d49acd2804525/java/com/google/security/wycheproof/testcases/EcdsaTest.java#L68-L84
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
    * Conversion form P1363 to ASN.1/DER
    *
    * P1363 contains two integer wothout separator, ASN.1 signature format looks like:
    *
    * {{{
    *   ECDSASignature ::= SEQUENCE {
    *     r INTEGER,
    *     s INTEGER
    *   }
    * }}}
    *
    * Explaination for DER encoding:
    *
    * - 0x30 - is a SEQUENCE
    * - 0x02 - is a INTEGER
    *
    * Additional padding required by the requirement to hold values larger than 128 bytes.
    *
    * The solution is inspired by: https://github.com/pauldijou/jwt-scala/blob/master/core/src/main/scala/JwtUtils.scala#L254-L290
    */
  def toDer: ECSignature = {
    val size = data.length

    val rawRb = data.safeCopyOfRange(0, size / 2).dropWhile(_ == 0)
    val rawSb = data.safeCopyOfRange(size / 2, size).dropWhile(_ == 0)

    // pad values
    val rb = if ((rawRb(0) & 0x80) > 0) rawRb.safePrepended(0x0.toByte) else rawRb
    val sb = if ((rawSb(0) & 0x80) > 0) rawSb.safePrepended(0x0.toByte) else rawSb

    val len = rb.length + sb.length + 4

    val intro = if (len >= 128) Array[Byte](0x30, 0x81.toByte) else Array[Byte](0x30)
    val first = intro safeAppendedAll Array[Byte](len.toByte, 0x02, rb.length.toByte)
    val second = Array[Byte](0x02, sb.length.toByte)

    ECSignature(first safeAppendedAll rb safeAppendedAll second safeAppendedAll sb)
  }
}
