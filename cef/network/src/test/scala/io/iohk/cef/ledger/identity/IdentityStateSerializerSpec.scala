package io.iohk.cef.ledger.identity

import akka.util.ByteString
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, MustMatchers}

class IdentityStateSerializerSpec extends FlatSpec with MustMatchers with PropertyChecks {

  behavior of "IdentityStateSerializer"

  it should "serialize in a lossless way" in {
    forAll { (values: Set[Array[Byte]]) =>
      val s = IdentityStateSerializer.byteStringSerializable
      s.deserialize(s.serialize(values.map(ByteString.apply))).map(_.toArray) mustBe values
    }
  }
}
