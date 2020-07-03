package io.iohk.atala.crypto

object ECUtils {
  def toHex(bigInt: BigInt): String = {
    bigInt.toString(16)
  }

  def toBigInt(hexEncoded: String): BigInt = {
    BigInt(hexEncoded, 16)
  }

  /**
    * Coordinates on secp256k1 are always positive and keys are encoded without the byte sign that Java uses for
    * encoding/decoding a big integer.
    */
  def toBigInt(bytes: Array[Byte]): BigInt = {
    BigInt(1, bytes)
  }

  def hexToBytes(hexEncoded: String): Array[Byte] = {
    toBigInt(hexEncoded).toByteArray
  }

  def bytesToHex(bytes: Array[Byte]): String = {
    bytes.map(ECUtils.byteToHex).mkString
  }

  private def byteToHex(b: Byte): String = {
    // Truncation is needed because ScalaJS will return negative values prefixed with "ffffff"
    val str = "%02x".format(b)
    str.substring(str.length - 2, str.length)
  }

  def toUnsignedByteArray(src: BigInt): Array[Byte] = {
    src.toByteArray.dropWhile(_ == 0)
  }
}
