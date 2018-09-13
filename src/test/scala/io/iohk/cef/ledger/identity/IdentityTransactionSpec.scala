package io.iohk.cef.ledger.identity

import akka.util.ByteString
import io.iohk.cef.builder.SigningKeyPairs
import io.iohk.cef.crypto.low.DigitalSignature
import org.scalatest.{EitherValues, FlatSpec, MustMatchers, OptionValues}

class IdentityTransactionSpec
    extends FlatSpec
    with SigningKeyPairs
    with EitherValues
    with OptionValues
    with MustMatchers {

  behavior of "IdentityTransaction"

  val dummySignature = new DigitalSignature(ByteString.empty)

  it should "throw an error when the tx is inconsistent with the state" in {
    val state = IdentityLedgerState(Map("one" -> Set(alice.public)))
    val claim = Claim("one", alice.public, IdentityTransaction.sign("one", alice.public, alice.`private`))
    val link = Link("two", bob.public, IdentityTransaction.sign("two", carlos.public, bob.`private`))
    val unlink1 = Unlink("one", bob.public, IdentityTransaction.sign("one", bob.public, alice.`private`))
    val unlink2 = Unlink("two", bob.public, IdentityTransaction.sign("one", bob.public, bob.`private`))

    claim(state).left.value mustBe IdentityTakenError("one")
    link(state).left.value mustBe IdentityNotClaimedError("two")
    unlink1(state).left.value mustBe PublicKeyNotAssociatedWithIdentity("one", bob.public)
    unlink2(state).left.value mustBe UnableToVerifySignatureError
  }

  it should "apply a claim" in {
    val state = IdentityLedgerState()
    val claim = Claim("one", alice.public, IdentityTransaction.sign("one", alice.public, alice.`private`))
    val newStateEither = claim(state)

    val newState = newStateEither.right.value
    newState.keys mustBe Set("one")
    newState.get("one") mustBe Some(Set(alice.public))
    newState.contains("one") mustBe true
    newState.contains("two") mustBe false
  }

  it should "apply a link" in {
    val state = IdentityLedgerState(Map("one" -> Set(alice.public)))
    val link = Link("one", bob.public, IdentityTransaction.sign("one", bob.public, alice.`private`))
    val newStateEither = link(state)

    val newState = newStateEither.right.value
    newState.keys mustBe Set("one")
    newState.get("one").value mustBe Set(alice.public, bob.public)
    newState.contains("one") mustBe true
    newState.contains("two") mustBe false
  }

  it should "fail to apply a claim if the signature can not be verified" in {
    val state = IdentityLedgerState(Map.empty)
    val transaction = Claim("one", alice.public, IdentityTransaction.sign("onee", alice.public, alice.`private`))

    val result = transaction(state).left.value
    result mustBe UnableToVerifySignatureError
  }

  it should "fail to apply a link if the signature can not be verified" in {
    val state = IdentityLedgerState(Map("one" -> Set(alice.public)))
    val link = Link("one", bob.public, IdentityTransaction.sign("one", bob.public, bob.`private`))

    val result = link(state).left.value
    result mustBe UnableToVerifySignatureError
  }

  it should "fail to apply an unlink if the signature can not be verified" in {
    val state = IdentityLedgerState(Map("one" -> Set(alice.public)))
    val transaction = Unlink("one", alice.public, IdentityTransaction.sign("onne", alice.public, alice.`private`))

    val result = transaction(state).left.value
    result mustBe UnableToVerifySignatureError
  }

  it should "apply an unlink" in {
    val state = IdentityLedgerState(Map("one" -> Set(daniel.public, alice.public)))
    val unlink1 = Unlink("one", daniel.public, IdentityTransaction.sign("one", daniel.public, daniel.`private`))
    val unlink2 = Unlink("one", alice.public, IdentityTransaction.sign("one", alice.public, alice.`private`))
    val stateAfter1 = unlink1(state).right.value
    val stateAfter2 = unlink2(stateAfter1).right.value

    stateAfter1.keys mustBe Set("one")
    stateAfter1.get("one").value mustBe Set(alice.public)
    stateAfter1.contains("one") mustBe true
    stateAfter1.contains("two") mustBe false

    stateAfter2.keys mustBe Set()
    stateAfter2.get("one") mustBe None
    stateAfter2.contains("one") mustBe false
    stateAfter2.contains("two") mustBe false
  }

  it should "have the correct keys per tx" in {
    val claim = Claim("one", alice.public, IdentityTransaction.sign("one", alice.public, alice.`private`))
    val link = Link("two", bob.public, IdentityTransaction.sign("two", bob.public, bob.`private`))
    val unlink = Unlink("two", bob.public, IdentityTransaction.sign("two", bob.public, bob.`private`))
    claim.partitionIds mustBe Set(claim.identity)
    link.partitionIds mustBe Set(link.identity)
    unlink.partitionIds mustBe Set(unlink.identity)
  }
}
