package io.iohk.atala.prism.crypto

import io.iohk.atala.prism.util.BigIntOps.toUnsignedByteArray
import io.iohk.atala.prism.util.{BigIntOps, BytesOps}

case class ECKeyPair(privateKey: ECPrivateKey, publicKey: ECPublicKey)

trait ECKey {
  def getEncoded: Array[Byte]

  final def getHexEncoded: String = {
    BytesOps.bytesToHex(getEncoded.toIndexedSeq)
  }

  override def hashCode(): Int = {
    getEncoded.hashCode()
  }

  override def equals(o: Any): Boolean = {
    o match {
      case other: ECKey => getEncoded.toVector == other.getEncoded.toVector
      case _ => false
    }
  }
}

trait ECPrivateKey extends ECKey {
  override final def getEncoded: Array[Byte] = {
    BigIntOps.toUnsignedByteArray(getD)
  }

  def getD: BigInt
}

trait ECPublicKey extends ECKey {

  /**
    * Returns the encoded public key.
    *
    * <p>The curve field size is fixed to 32 bytes (256 bits), and this method will encode the public key in 65 bytes as
    * follows:
    * <ul>
    *   <li> Byte 0: 4, a hard-coded value indicating this encoding is an uncompressed concatenation of the curve
    *        coordinates.
    *   <li> Bytes 1-33: the raw value of the x coordinate, padded with zeroes.
    *   <li> Bytes 34-65: the raw value of the y coordinate, padded with zeroes.
    * </ul>
    */
  override final def getEncoded: Array[Byte] = {
    val curvePoint = getCurvePoint
    val size = ECConfig.CURVE_FIELD_BYTE_SIZE
    val xArr = toUnsignedByteArray(curvePoint.x)
    val yArr = toUnsignedByteArray(curvePoint.y)
    if (xArr.length <= size && yArr.length <= size) {
      val arr = new Array[Byte](1 + 2 * size)
      arr(0) = 4 // Uncompressed point indicator for encoding
      Array.copy(xArr, 0, arr, size - xArr.length + 1, xArr.length)
      Array.copy(yArr, 0, arr, arr.length - yArr.length, yArr.length)
      arr
    } else {
      throw new RuntimeException("Point coordinates do not match field size")
    }
  }

  def getCurvePoint: ECPoint
}

case class ECPoint(x: BigInt, y: BigInt)
