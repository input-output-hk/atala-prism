package io.iohk.cef.ledger.identity

import io.iohk.cef.builder.RSAKeyGenerator
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, MustMatchers}

class IdentityStateSerializerSpec extends FlatSpec with MustMatchers with RSAKeyGenerator with PropertyChecks {

  behavior of "IdentityStateSerializer"

  it should "serialize in a lossless way" in {
    forAll { (values: List[Int]) =>
      val keys = values.map(_ => generateKeyPair._1).toSet
      val s = IdentityStateSerializer.byteStringSerializable
      s.decode(s.encode(keys)) mustBe Some(keys)
    }
  }
}
