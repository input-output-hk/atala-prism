package io.iohk.cef.ledger.identity

import io.iohk.cef.builder.SigningKeyPairs
import io.iohk.cef.frontend.models.IdentityTransactionType
import org.scalatest.{EitherValues, FlatSpec, MustMatchers, OptionValues}

class IdentityTransactionSpec
    extends FlatSpec
    with SigningKeyPairs
    with EitherValues
    with OptionValues
    with MustMatchers {

  behavior of "IdentityTransaction"

  it should "throw an error when the tx is inconsistent with the state" in {
    val state = IdentityLedgerState(Map("one" -> IdentityData.forKeys(alice.public)))
    val claim = Claim(
      "one",
      alice.public,
      IdentityTransaction.sign("one", IdentityTransactionType.Claim, alice.public, alice.`private`))
    val link = Link(
      "two",
      bob.public,
      IdentityTransaction.sign("two", IdentityTransactionType.Link, carlos.public, bob.`private`),
      IdentityTransaction.sign("two", IdentityTransactionType.Link, carlos.public, carlos.`private`)
    )

    val unlink1 = Unlink(
      "one",
      bob.public,
      IdentityTransaction.sign("one", IdentityTransactionType.Unlink, bob.public, alice.`private`))
    val unlink2 = Unlink(
      "two",
      bob.public,
      IdentityTransaction.sign("one", IdentityTransactionType.Unlink, bob.public, bob.`private`))

    claim(state).left.value mustBe IdentityTakenError("one")
    link(state).left.value mustBe IdentityNotClaimedError("two")
    unlink1(state).left.value mustBe PublicKeyNotAssociatedWithIdentity("one", bob.public)
    unlink2(state).left.value mustBe UnableToVerifySignatureError
  }

  it should "apply a claim" in {
    val state = IdentityLedgerState()
    val claim = Claim(
      "one",
      alice.public,
      IdentityTransaction.sign("one", IdentityTransactionType.Claim, alice.public, alice.`private`))
    val newStateEither = claim(state)

    val newState = newStateEither.right.value
    newState.keys mustBe Set("one")
    newState.get("one") mustBe Some(IdentityData.forKeys(alice.public))
    newState.contains("one") mustBe true
    newState.contains("two") mustBe false
  }

  it should "apply a link" in {
    val state = IdentityLedgerState(Map("one" -> IdentityData.forKeys(alice.public)))
    val link = Link(
      "one",
      bob.public,
      IdentityTransaction.sign("one", IdentityTransactionType.Link, bob.public, alice.`private`),
      IdentityTransaction.sign("one", IdentityTransactionType.Link, bob.public, bob.`private`)
    )

    val newStateEither = link(state)

    val newState = newStateEither.right.value
    newState.keys mustBe Set("one")
    newState.get("one").value mustBe IdentityData.forKeys(alice.public, bob.public)
    newState.contains("one") mustBe true
    newState.contains("two") mustBe false
  }

  it should "fail to apply a claim if the signature can not be verified" in {
    val state = IdentityLedgerState(Map.empty)
    val transaction = Claim(
      "one",
      alice.public,
      IdentityTransaction.sign("onee", IdentityTransactionType.Claim, alice.public, alice.`private`))

    val result = transaction(state).left.value
    result mustBe UnableToVerifySignatureError
  }

  it should "fail to apply a link if the signature can not be verified" in {
    val state = IdentityLedgerState(Map("one" -> IdentityData.forKeys(alice.public)))
    val link =
      Link(
        "one",
        bob.public,
        IdentityTransaction.sign("one", IdentityTransactionType.Link, bob.public, bob.`private`),
        IdentityTransaction.sign("one", IdentityTransactionType.Link, bob.public, bob.`private`)
      )

    val result = link(state).left.value
    result mustBe UnableToVerifySignatureError
  }

  it should "fail to apply a link if the link identity signature can not be verified" in {
    val state = IdentityLedgerState(Map("one" -> IdentityData.forKeys(alice.public)))
    val link =
      Link(
        "one",
        bob.public,
        IdentityTransaction.sign("one", IdentityTransactionType.Link, bob.public, alice.`private`),
        IdentityTransaction.sign("one", IdentityTransactionType.Link, bob.public, alice.`private`)
      )

    val result = link(state).left.value
    result mustBe UnableToVerifyLinkingIdentitySignatureError("one", bob.public)
  }

  it should "fail to apply an unlink if the signature can not be verified" in {
    val state = IdentityLedgerState(Map("one" -> IdentityData.forKeys(alice.public)))
    val transaction = Unlink(
      "one",
      alice.public,
      IdentityTransaction.sign("onne", IdentityTransactionType.Unlink, alice.public, alice.`private`))

    val result = transaction(state).left.value
    result mustBe UnableToVerifySignatureError
  }

  it should "apply an unlink" in {
    val state = IdentityLedgerState(Map("one" -> IdentityData.forKeys(daniel.public, alice.public)))
    val unlink1 = Unlink(
      "one",
      daniel.public,
      IdentityTransaction.sign("one", IdentityTransactionType.Unlink, daniel.public, daniel.`private`))
    val unlink2 = Unlink(
      "one",
      alice.public,
      IdentityTransaction.sign("one", IdentityTransactionType.Unlink, alice.public, alice.`private`))
    val stateAfter1 = unlink1(state).right.value
    val stateAfter2 = unlink2(stateAfter1).right.value

    stateAfter1.keys mustBe Set("one")
    stateAfter1.get("one").value mustBe IdentityData.forKeys(alice.public)
    stateAfter1.contains("one") mustBe true
    stateAfter1.contains("two") mustBe false

    stateAfter2.keys mustBe Set()
    stateAfter2.get("one") mustBe None
    stateAfter2.contains("one") mustBe false
    stateAfter2.contains("two") mustBe false
  }

  it should "have the correct keys per tx" in {
    val claim = Claim(
      "one",
      alice.public,
      IdentityTransaction.sign("one", IdentityTransactionType.Claim, alice.public, alice.`private`))
    val link =
      Link(
        "two",
        bob.public,
        IdentityTransaction.sign("two", IdentityTransactionType.Link, bob.public, bob.`private`),
        IdentityTransaction.sign("two", IdentityTransactionType.Link, bob.public, bob.`private`)
      )
    val unlink = Unlink(
      "two",
      bob.public,
      IdentityTransaction.sign("two", IdentityTransactionType.Unlink, bob.public, bob.`private`))
    claim.partitionIds mustBe Set(claim.identity)
    link.partitionIds mustBe Set(link.identity)
    unlink.partitionIds mustBe Set(unlink.identity)
  }

  it should "apply a Endorse" in {
    val state =
      IdentityLedgerState(Map("one" -> IdentityData.forKeys(alice.public), "two" -> IdentityData.forKeys(bob.public)))
    val endorse = Endorse(
      "two",
      bob.public,
      IdentityTransaction.sign("two", IdentityTransactionType.Endorse, bob.public, bob.`private`),
      "one")

    val newStateEither = endorse(state)
    val newState = newStateEither.right.value
    newState.keys mustBe Set("one", "two")
    newState.get("one").value.endorsers.size mustBe 1
    newState.get("two").value.endorsers.size mustBe 0
    newState.get("one").value.endorsers.contains("two")
  }

  it should "fail to apply a endorse identity if the identity is not claimed" in {
    val state =
      IdentityLedgerState(Map("two" -> IdentityData.forKeys(bob.public)))
    val transaction = Endorse(
      "two",
      bob.public,
      IdentityTransaction.sign("two", IdentityTransactionType.Endorse, bob.public, bob.`private`),
      "one")

    val result = transaction(state).left.value
    result mustBe UnknownEndorsedIdentityError("one")
  }

  it should "fail to endorse identity if the endorser identity is not claimed" in {
    val state =
      IdentityLedgerState(Map("two" -> IdentityData.forKeys(bob.public)))
    val transaction = Endorse(
      "three",
      bob.public,
      IdentityTransaction.sign("three", IdentityTransactionType.Endorse, bob.public, bob.`private`),
      "two")

    val result = transaction(state).left.value
    result mustBe UnknownEndorserIdentityError("three")
  }

  it should "fail to endorse identity if the endorser signature is not valid" in {
    val state =
      IdentityLedgerState(Map("one" -> IdentityData.forKeys(alice.public), "two" -> IdentityData.forKeys(bob.public)))
    val signature = IdentityTransaction.sign("two", IdentityTransactionType.Endorse, bob.public, alice.`private`)
    val transaction = Endorse("two", bob.public, signature, "one")

    val result = transaction(state).left.value
    result mustBe UnableToVerifyEndorserSignatureError("two", signature)
  }

  it should "fail to endorse identity if the endorser key is not valid" in {
    val state =
      IdentityLedgerState(Map("one" -> IdentityData.forKeys(alice.public), "two" -> IdentityData.forKeys(bob.public)))
    val signature = IdentityTransaction.sign("two", IdentityTransactionType.Endorse, daniel.public, daniel.`private`)
    val transaction = Endorse("two", daniel.public, signature, "one")

    val result = transaction(state).left.value
    result mustBe PublicKeyNotAssociatedWithIdentity("two", daniel.public)
  }

}
