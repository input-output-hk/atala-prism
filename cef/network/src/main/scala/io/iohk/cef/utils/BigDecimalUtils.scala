package io.iohk.cef.utils

import io.iohk.cef.protobuf.Common.DecimalProto

object BigDecimalUtils {
  private val lowBits = 64
  private val hiBits = 32

  def fromProto(decimalProto: DecimalProto): BigDecimal = {
    val hi = decimalProto.hi.map(BigDecimal(_) * BigDecimal(2).pow(lowBits))
    val number = BigDecimal(decimalProto.lo) + hi.getOrElse(BigDecimal(0))
    val decimal = number / BigDecimal(10).pow(decimalProto.scale)
    decimal * (if (decimalProto.negative) -1 else 1)
  }

  def toProto(bigDecimal: BigDecimal): DecimalProto = {
    if(bigDecimal >= BigDecimal(2).pow(lowBits + hiBits)) {
      throw new IllegalArgumentException(s"Cannot represent BigDecimal in proto: ${bigDecimal}")
    }
    val hif = BigDecimal(2).pow(lowBits)
    val hi = if (bigDecimal >= hif) Some((bigDecimal / hif).toBigInt()) else None
    val lo = bigDecimal - BigDecimal((hi.getOrElse(BigInt(0)) * hif.toBigInt()))
    DecimalProto(lo.toLong, hi.map(BigDecimal(_).toInt), bigDecimal < 0, bigDecimal.scale)
  }
}
