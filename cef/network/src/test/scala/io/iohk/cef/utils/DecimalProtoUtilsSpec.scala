package io.iohk.cef.utils

import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, MustMatchers}

class DecimalProtoUtilsSpec extends FlatSpec with MustMatchers with PropertyChecks {

  behavior of "BigDecimalUtils"

  it should "convert back and forth from Protos without losing precision" in {
    forAll { (b: BigDecimal) =>
      whenever(bigDecimalWithinRange(b)){
        DecimalProtoUtils.fromProto(DecimalProtoUtils.toProto(b)) mustBe b
      }
    }
  }

  private def bigDecimalWithinRange(b: BigDecimal) = {
    b.scale <= DecimalProtoUtils.MaxScale && b <= DecimalProtoUtils.MaxNumber && b >= DecimalProtoUtils.MinNumber
  }
}
