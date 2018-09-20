package io.iohk.cef.utils

import io.iohk.cef.protobuf.Common.DecimalProto

object DecimalProtoUtils {
  val MaxNumberLow = BigDecimal(Long.MaxValue)
  val MaxNumberHigh = BigDecimal(Long.MaxValue)
  val MaxNumber = MaxNumberLow + MaxNumberHigh
  val MinNumber = BigDecimal(Long.MinValue) * 2
  val MaxScale = 37

  def fromProto(decimalProto: DecimalProto): BigDecimal = {
    val number = (BigDecimal(decimalProto.hi) * (MaxNumberLow + 1)) + BigDecimal(decimalProto.lo)
    number / BigDecimal(10).pow(decimalProto.scale)
  }

  def toProto(bigDecimal: BigDecimal): DecimalProto = {
    val absBigDecimal = bigDecimal.abs
    val signMultiplier = if (bigDecimal < 0) -1L else 1L
    val intRepresentation = absBigDecimal * BigDecimal(10).pow(bigDecimal.scale)
    if (absBigDecimal.scale > MaxScale || intRepresentation >= MaxNumber) {
      throw new IllegalArgumentException(
        s"Cannot represent BigDecimal in proto: ${bigDecimal}. Scale: ${bigDecimal.scale}"
      )
    }
    val hi = if (intRepresentation > MaxNumberLow) {
      (intRepresentation / (MaxNumberLow + 1)).toLong
    } else {
      0L
    }
    val lo = intRepresentation - (hi * (MaxNumberLow + 1))
    DecimalProto(signMultiplier * lo.toLong, signMultiplier * hi.toLong, bigDecimal.scale)
  }
}
