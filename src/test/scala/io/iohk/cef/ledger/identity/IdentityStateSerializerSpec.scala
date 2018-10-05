package io.iohk.cef.ledger.identity

import io.iohk.cef.crypto._
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, MustMatchers}

class IdentityStateSerializerSpec extends FlatSpec with MustMatchers with PropertyChecks {

  private val key1 = generateSigningKeyPair().public
  private val key2 = generateSigningKeyPair().public
  private val s = IdentityStateSerializer.byteStringSerializable

  behavior of "IdentityStateSerializer"

  it should "serialize in a lossless way" in {
    s.decode(s.encode(Set())) mustBe Some(Set())
    s.decode(s.encode(Set(key1))) mustBe Some(Set(key1))
    s.decode(s.encode(Set(key1, key2))) mustBe Some(Set(key1, key2))
  }
}
