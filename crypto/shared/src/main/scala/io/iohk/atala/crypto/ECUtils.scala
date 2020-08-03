package io.iohk.atala.crypto

import io.iohk.atala.util.ArrayOps._

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
    require(hexEncoded.length % 2 == 0, "Hex length needs to be even")
    hexEncoded.grouped(2).toVector.map(hexToByte).toByteArray
  }

  def bytesToHex(bytes: Array[Byte]): String = {
    bytes.toVector.map(ECUtils.byteToHex).mkString
  }

  private def byteToHex(b: Byte): String = {
    // Ensure only the last byte is used for formatting (needed in JavaScript)
    "%02x".format(b & 0xff)
  }

  private def hexToByte(h: String): Byte = {
    Integer.parseInt(h, 16).toByte
  }

  def toUnsignedByteArray(src: BigInt): Array[Byte] = {
    src.toByteArray.dropWhile(_ == 0)
  }
}
