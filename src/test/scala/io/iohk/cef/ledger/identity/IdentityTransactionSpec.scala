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
    val pair1 = generateKeyPair
    val pair2 = generateKeyPair
    val pair3 = generateKeyPair

    val state = IdentityLedgerState(Map("one" -> Set(pair1._1)))
    val claim = Claim("one", pair1._1)
    val link = Link("two", pair2._1, Link.sign("two", pair3._1, pair2._2))
    val unlink1 = Unlink("two", pair2._1)
    val unlink2 = Unlink("one", pair2._1)

    claim(state).left.value mustBe IdentityTakenError("one")
    link(state).left.value mustBe IdentityNotClaimedError("two")
    unlink1(state).left.value mustBe PublicKeyNotAssociatedWithIdentity("two", pair2._1)
    unlink2(state).left.value mustBe PublicKeyNotAssociatedWithIdentity("one", pair2._1)
  }

  it should "apply a claim" in {
    val pair1 = generateKeyPair

    val state = IdentityLedgerState()
    val claim = Claim("one", pair1._1)
    val newStateEither = claim(state)

    val newState = newStateEither.right.value
    newState.keys mustBe Set("one")
    newState.get("one") mustBe Some(Set(pair1._1))
    newState.contains("one") mustBe true
    newState.contains("two") mustBe false
  }

  it should "apply a link" in {
    val pair1 = generateKeyPair
    val pair2 = generateKeyPair

    val state = IdentityLedgerState(Map("one" -> Set(pair1._1)))
    val link = Link("one", pair2._1, Link.sign("one", pair2._1, pair1._2))
    val newStateEither = link(state)

    val newState = newStateEither.right.value
    newState.keys mustBe Set("one")
    newState.get("one").value mustBe Set(pair1._1, pair2._1)
    newState.contains("one") mustBe true
    newState.contains("two") mustBe false
  }

  it should "fail to apply a link if the signature can not be verified" in {
    val pair1 = generateKeyPair
    val pair2 = generateKeyPair

    val state = IdentityLedgerState(Map("one" -> Set(pair1._1)))
    val link = Link("one", pair2._1, Link.sign("one", pair2._1, pair2._2))

    val result = link(state).left.value
    result mustBe UnableToVerifySignatureError
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
    val pair1 = generateKeyPair
    val pair2 = generateKeyPair

    val claim = Claim("one", pair1._1)
    val link = Link("two", pair2._1, Link.sign("two", pair2._1, pair2._2))
    val unlink = Unlink("two", pair2._1)
    claim.partitionIds mustBe Set(claim.identity)
    link.partitionIds mustBe Set(link.identity)
    unlink.partitionIds mustBe Set(unlink.identity)
  }
}
