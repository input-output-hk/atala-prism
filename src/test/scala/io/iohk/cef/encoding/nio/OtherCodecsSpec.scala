package io.iohk.cef.encoding.nio
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class OtherCodecsSpec extends FlatSpec {

  behavior of "OtherCodecs"

  they should "enable encoding/decoding of BigDecimal" in {
    forAll(arbitrary[BigDecimal]) { d =>
      NioDecoder[BigDecimal].decode(NioEncoder[BigDecimal].encode(d)) shouldBe Some(d)
    }
  }

  they should "enable encoding/decoding of Maps" in {
    forAll(arbitrary[Map[String, String]]) { m =>
      NioDecoder[Map[String, String]].decode(NioEncoder[Map[String, String]].encode(m)) shouldBe Some(m)
    }
  }
}
