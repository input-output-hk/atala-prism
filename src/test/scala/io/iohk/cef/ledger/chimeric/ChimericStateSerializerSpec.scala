package io.iohk.cef.ledger.chimeric
import org.scalacheck.Arbitrary
import org.scalatest
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, MustMatchers}

class ChimericStateSerializerSpec extends FlatSpec with MustMatchers with PropertyChecks {

  behavior of "ChimericStateSerializer"

  it should "serialize without losing infomation" in {
    val serializer = ChimericStateSerializer.byteStringSerializable
    implicit val arb = Arbitrary(ChimericGenerators.StateValueGen)
    forAll { (state: ChimericStateValue) =>
      serializer.decode(serializer.encode(state)) mustBe Some(state)
    }
  }

  it should "handle numbers larger than doubles" in {

    val serializer = ChimericStateSerializer.byteStringSerializable
    def test(number: BigDecimal): scalatest.Assertion = {
      val state = ValueHolder(Value("CRC" -> number))
      val serializedDecimal = serializer.encode(state)
      serializer.decode(serializedDecimal) mustBe Some(state)
    }

    test(BigDecimal(Double.MaxValue) + 0.1)
    test(BigDecimal(Double.MinValue) - 0.1)
    test(BigDecimal(Float.MaxValue.toDouble) + 0.1)
    test(BigDecimal(Float.MinValue.toDouble) - 0.1)
    test(BigDecimal(Double.MaxValue).pow(20) + 0.1)
    test(BigDecimal(Double.MinValue).pow(20) * -1 - 0.1)
  }
}
