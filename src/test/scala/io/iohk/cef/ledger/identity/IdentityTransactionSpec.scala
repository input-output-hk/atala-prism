package io.iohk.cef.ledger.identity

import akka.util.ByteString
import org.scalatest.{FlatSpec, MustMatchers}

class IdentityTransactionSpec extends FlatSpec with MustMatchers {

  behavior of "IdentityTransaction"

  it should "throw an error when the tx is inconsistent with the state" in {
    val state = IdentityLedgerState(Map("one" -> Set(ByteString("one"))))
    val claim = Claim("one", ByteString("one"))
    val link = Link("two", ByteString("two"))
    val unlink1 = Unlink("two", ByteString("two"))
    val unlink2 = Unlink("one", ByteString("two"))
    claim(state) mustBe Left(IdentityTakenError("one"))
    link(state) mustBe Left(IdentityNotClaimedError("two"))
    unlink1(state) mustBe Left(PublicKeyNotAssociatedWithIdentity("two", ByteString("two")))
    unlink2(state) mustBe Left(PublicKeyNotAssociatedWithIdentity("one", ByteString("two")))
  }

  it should "apply a claim" in {
    val state = IdentityLedgerState()
    val claim = Claim("one", ByteString("one"))
    val newStateEither = claim(state)
    newStateEither.isRight mustBe true
    val newState = newStateEither.right.get
    newState.keys mustBe Set("one")
    newState.get("one") mustBe Some(Set(ByteString("one")))
    newState.contains("one") mustBe true
    newState.contains("two") mustBe false
  }

  it should "apply a link" in {
    val state = IdentityLedgerState(Map("one" -> Set(ByteString("one"))))
    val link = Link("one", ByteString("two"))
    val newStateEither = link(state)
    newStateEither.isRight mustBe true
    val newState = newStateEither.right.get
    newState.keys mustBe Set("one")
    newState.get("one") mustBe Some(Set(ByteString("one"), ByteString("two")))
    newState.contains("one") mustBe true
    newState.contains("two") mustBe false
  }

  it should "apply an unlink" in {
    val state = IdentityLedgerState(Map("one" -> Set(ByteString("one"), ByteString("two"))))
    val unlink1 = Unlink("one", ByteString("one"))
    val unlink2 = Unlink("one", ByteString("two"))
    val stateAfter1Either = unlink1(state)
    stateAfter1Either.isRight mustBe true
    val stateAfter1 = stateAfter1Either.right.get
    val stateAfter2Either = unlink2(stateAfter1)
    stateAfter2Either.isRight mustBe true
    val stateAfter2 = stateAfter2Either.right.get
    stateAfter1.keys mustBe Set("one")
    stateAfter1.get("one") mustBe Some(Set(ByteString("two")))
    stateAfter1.contains("one") mustBe true
    stateAfter1.contains("two") mustBe false
    stateAfter2.keys mustBe Set()
    stateAfter2.get("one") mustBe None
    stateAfter2.contains("one") mustBe false
    stateAfter2.contains("two") mustBe false
  }

  it should "have the correct keys per tx" in {
    val claim = Claim("one", ByteString("one"))
    val link = Link("two", ByteString("two"))
    val unlink = Unlink("two", ByteString("two"))
    claim.partitionIds mustBe Set(claim.identity)
    link.partitionIds mustBe Set(link.identity)
    unlink.partitionIds mustBe Set(unlink.identity)
  }
}
