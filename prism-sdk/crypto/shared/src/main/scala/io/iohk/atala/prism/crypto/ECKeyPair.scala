package io.iohk.atala.prism.crypto

import io.iohk.atala.prism.util
import io.iohk.atala.prism.util.BigIntOps.{cordBytesWithPaddedZeros, toUnsignedByteArray}
import io.iohk.atala.prism.util.{BigIntOps, BytesOps}

case class ECKeyPair(privateKey: ECPrivateKey, publicKey: ECPublicKey)

trait ECKey {
  def getEncoded: Array[Byte]

  final def getHexEncoded: String = {
    BytesOps.bytesToHex(getEncoded)
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

  override def toString: String = {
    util.StringUtils.masked(getHexEncoded)
  }
}

trait ECPublicKey extends ECKey {

  /**
    * Returns the encoded public key.
    *
    * <p>The curve field size is fixed to 32 bytes (256 bits), and this method will encode the public key in 65 bytes as
    * follows:
    * <ul>
    *   <li> Byte 1: 4, a hard-coded value indicating this encoding is an uncompressed concatenation of the curve coordinates. </li>
    *   <li> Bytes 2-33: the raw value of the x coordinate, padded with zeroes. </li>
    *   <li> Bytes 34-65: the raw value of the y coordinate, padded with zeroes. </li>
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
      System.arraycopy(xArr, 0, arr, size - xArr.length + 1, xArr.length)
      System.arraycopy(yArr, 0, arr, arr.length - yArr.length, yArr.length)
      arr
    } else {
      throw new RuntimeException("Point coordinates do not match field size")
    }
  }

  /**
    * Returns the curve point of secp256k1 curve.
    */
  def getCurvePoint: ECPoint

  /**
    * Returns the encoded and compressed public key.
    *
    * <p>The curve field size is fixed to 32 bytes (256 bits), and this method will encode the public key in 33 bytes as
    * follows:
    * <ul>
    *   <li> Byte 1: 3 or 2, a dynamic value used in the future to find y coordinate. </li>
    *   <li> Bytes 2-33: the raw value of the x coordinate, padded with zeros. </li>
    * </ul>
    */
  final def getCompressed: Array[Byte] = {
    val curvePoint = getCurvePoint
    val size = ECConfig.CURVE_FIELD_BYTE_SIZE
    val yArr = toUnsignedByteArray(curvePoint.y)
    val xArr = cordBytesWithPaddedZeros(curvePoint.x)
    val prefix = 2 + (yArr(yArr.length - 1) & 1)
    val arr = new Array[Byte](1 + size)
    arr.update(0, prefix.toByte)
    System.arraycopy(xArr, 0, arr, 1, xArr.length)
    arr
  }

  override def toString: String = getHexEncoded
}

/**
  * Contains coordinates x and y of secp256k1 curve.
  *
  * <p> Each coordinate size is fixed to 32 bytes.
  */
case class ECPoint(x: BigInt, y: BigInt)
