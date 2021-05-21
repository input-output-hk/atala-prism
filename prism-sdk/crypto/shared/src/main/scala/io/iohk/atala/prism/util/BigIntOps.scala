package io.iohk.atala.prism.util

import io.iohk.atala.prism.crypto.ECConfig

import scala.util.control.Breaks

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
    val array = src.toByteArray
    var result = new Array[Byte](0)
    Breaks.breakable {
      for (i <- array.indices) {
        if (array(i) != 0) {
          result = new Array[Byte](array.length - i)
          for (j <- i until array.length) {
            result(j - i) = array(j)
          }
          Breaks.break()
        }
      }
    }
    result
  }

  def cordBytesWithPaddedZeros(src: BigInt): Array[Byte] = {
    val cordActualArray = toUnsignedByteArray(src)
    if (cordActualArray.length < ECConfig.CURVE_FIELD_BYTE_SIZE) {
      val actualSize = cordActualArray.length
      val zerosSize = ECConfig.CURVE_FIELD_BYTE_SIZE - actualSize
      val zeros: Seq[Byte] = for (_ <- 1 to zerosSize) yield 0
      cordActualArray.prependedAll(zeros)
    } else cordActualArray
  }
}
