package io.iohk.cef.ledger.chimeric
import org.scalacheck.Arbitrary
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, MustMatchers}

class ChimericStateSerializerSpec extends FlatSpec with MustMatchers with PropertyChecks {

  behavior of "ChimericStateSerializer"

  it should "serialize without losing infomation" in {
    val serializer = ChimericStateSerializer.byteStringSerializableUsingString
    implicit val arb = Arbitrary(ChimericGenerators.StateValueGen)
    forAll { (state: ChimericStateValue) =>
      serializer.deserialize(serializer.serialize(state)) mustBe state
    }
  }
}
