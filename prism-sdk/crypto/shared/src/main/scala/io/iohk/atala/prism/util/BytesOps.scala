package io.iohk.atala.prism.util

import io.iohk.atala.prism.util.ArrayOps._

object BytesOps {
  def hexToBytes(hexEncoded: String): Array[Byte] = {
    require(hexEncoded.length % 2 == 0, "Hex length needs to be even")
    hexEncoded.grouped(2).toVector.map(hexToByte).toByteArray
  }

  def bytesToHex(bytes: Array[Byte]): String = {
    bytes.toVector.map(byteToHex).mkString
  }

  private def byteToHex(b: Byte): String = {
    // Ensure only the last byte is used for formatting (needed in JavaScript)
    "%02x".format(b & 0xff)
  }

  private def hexToByte(h: String): Byte = {
    Integer.parseInt(h, 16).toByte
  }
}
