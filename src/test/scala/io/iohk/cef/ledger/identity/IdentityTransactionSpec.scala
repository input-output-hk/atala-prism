package io.iohk.cef.ledger.identity

import io.iohk.cef.builder.RSAKeyGenerator
import org.scalatest.{EitherValues, FlatSpec, MustMatchers, OptionValues}

class IdentityTransactionSpec
    extends FlatSpec
    with RSAKeyGenerator
    with EitherValues
    with OptionValues
    with MustMatchers {

  behavior of "IdentityTransaction"

  it should "throw an error when the tx is inconsistent with the state" in {
    val keys = (1 to 4).map(_ => generateKeyPair._1)

    val state = IdentityLedgerState(Map("one" -> Set(keys(0))))
    val claim = Claim("one", keys(0))
    val link = Link("two", keys(1))
    val unlink1 = Unlink("two", keys(1))
    val unlink2 = Unlink("one", keys(1))

    claim(state).left.value mustBe IdentityTakenError("one")
    link(state).left.value mustBe IdentityNotClaimedError("two")
    unlink1(state).left.value mustBe PublicKeyNotAssociatedWithIdentity("two", keys(1))
    unlink2(state).left.value mustBe PublicKeyNotAssociatedWithIdentity("one", keys(1))
  }

  it should "apply a claim" in {
    val keys = (1 to 4).map(_ => generateKeyPair._1)

    val state = IdentityLedgerState()
    val claim = Claim("one", keys(0))
    val newStateEither = claim(state)

    val newState = newStateEither.right.value
    newState.keys mustBe Set("one")
    newState.get("one") mustBe Some(Set(keys(0)))
    newState.contains("one") mustBe true
    newState.contains("two") mustBe false
  }

  it should "apply a link" in {
    val keys = (1 to 4).map(_ => generateKeyPair._1)

    val state = IdentityLedgerState(Map("one" -> Set(keys(0))))
    val link = Link("one", keys(1))
    val newStateEither = link(state)

    val newState = newStateEither.right.value
    newState.keys mustBe Set("one")
    newState.get("one") mustBe Some(keys.take(2).toSet)
    newState.contains("one") mustBe true
    newState.contains("two") mustBe false
  }

  it should "apply an unlink" in {
    val keys = (1 to 4).map(_ => generateKeyPair._1)

    val state = IdentityLedgerState(Map("one" -> keys.take(2).toSet))
    val unlink1 = Unlink("one", keys(0))
    val unlink2 = Unlink("one", keys(1))
    val stateAfter1 = unlink1(state).right.value
    val stateAfter2 = unlink2(stateAfter1).right.value

    stateAfter1.keys mustBe Set("one")
    stateAfter1.get("one").value mustBe Set(keys(1))
    stateAfter1.contains("one") mustBe true
    stateAfter1.contains("two") mustBe false

    stateAfter2.keys mustBe Set()
    stateAfter2.get("one") mustBe None
    stateAfter2.contains("one") mustBe false
    stateAfter2.contains("two") mustBe false
  }

  it should "have the correct keys per tx" in {
    val keys = (1 to 4).map(_ => generateKeyPair._1)

    val claim = Claim("one", keys(0))
    val link = Link("two", keys(1))
    val unlink = Unlink("two", keys(1))
    claim.partitionIds mustBe Set(claim.identity)
    link.partitionIds mustBe Set(link.identity)
    unlink.partitionIds mustBe Set(unlink.identity)
  }
}
