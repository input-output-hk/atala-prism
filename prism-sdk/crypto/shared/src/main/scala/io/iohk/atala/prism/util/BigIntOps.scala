package io.iohk.atala.prism.util

object BigIntOps {
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

  def toUnsignedByteArray(src: BigInt): Array[Byte] = {
    src.toByteArray.dropWhile(_ == 0)
  }
}
