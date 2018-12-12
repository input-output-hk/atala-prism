package io.iohk.cef.ledger.identity

import io.iohk.cef.builder.SigningKeyPairs
import io.iohk.cef.crypto._
import io.iohk.cef.crypto.certificates.{CachedCertificatePair, _}
import org.scalatest.EitherValues._
import org.scalatest.FlatSpec
import org.scalatest.MustMatchers._
import org.scalatest.OptionValues._

class IdentityTransactionSpec extends FlatSpec with SigningKeyPairs {

  import ExampleCertificates._

  private def aliceClaimData(identity: String) = ClaimData(identity, alice.public)
  private def aliceUnlinkData(identity: String) = UnlinkData(identity, alice.public)
  private def bobLinkData(identity: String) = LinkData(identity, bob.public)
  private def bobUnlinkData(identity: String) = UnlinkData(identity, bob.public)
  private def danielUnlinkData(identity: String) = UnlinkData(identity, daniel.public)

  behavior of "IdentityTransaction"

  it should "throw an error when the tx is inconsistent with the state" in {
    val state = IdentityLedgerState(Map("one" -> IdentityData.forKeys(alice.public)))
    val claim = aliceClaimData("one").toTransaction(alice.`private`)
    val link = bobLinkData("two").toTransaction(bob.`private`, carlos.`private`)
    val unlink1 = bobUnlinkData("one").toTransaction(alice.`private`)
    val unlink2 = bobUnlinkData("two").toTransaction(bob.`private`)

    claim(state).left.value mustBe IdentityTakenError("one")
    link(state).left.value mustBe IdentityNotClaimedError("two")
    unlink1(state).left.value mustBe PublicKeyNotAssociatedWithIdentity("one", bob.public)
    unlink2(state).left.value mustBe UnableToVerifySignatureError
  }

  it should "apply a claim" in {
    val state = IdentityLedgerState()
    val claim = aliceClaimData("one").toTransaction(alice.`private`)
    val newStateEither = claim(state)

    val newState = newStateEither.right.value
    newState.keys mustBe Set("one")
    newState.get("one") mustBe Some(IdentityData.forKeys(alice.public))
    newState.contains("one") mustBe true
    newState.contains("two") mustBe false
  }

  it should "apply a link" in {
    val state = IdentityLedgerState(Map("one" -> IdentityData.forKeys(alice.public)))
    val link = bobLinkData("one").toTransaction(alice.`private`, bob.`private`)

    val newStateEither = link(state)

    val newState = newStateEither.right.value
    newState.keys mustBe Set("one")
    newState.get("one").value mustBe IdentityData.forKeys(alice.public, bob.public)
    newState.contains("one") mustBe true
    newState.contains("two") mustBe false
  }

  it should "fail to apply a claim if the signature can not be verified" in {
    val state = IdentityLedgerState(Map.empty)
    val transaction = aliceClaimData("one").toTransaction(bob.`private`)

    val result = transaction(state).left.value
    result mustBe UnableToVerifySignatureError
  }

  it should "fail to apply a link if the signature can not be verified" in {
    val state = IdentityLedgerState(Map("one" -> IdentityData.forKeys(alice.public)))
    val link = bobLinkData("one").toTransaction(bob.`private`, bob.`private`)

    val result = link(state).left.value
    result mustBe UnableToVerifySignatureError
  }

  it should "fail to apply a link if the link identity signature can not be verified" in {
    val state = IdentityLedgerState(Map("one" -> IdentityData.forKeys(alice.public)))
    val link = bobLinkData("one").toTransaction(alice.`private`, alice.`private`)

    val result = link(state).left.value
    result mustBe UnableToVerifyLinkingIdentitySignatureError("one", bob.public)
  }

  it should "fail to apply an unlink if the signature can not be verified" in {
    val state = IdentityLedgerState(Map("one" -> IdentityData.forKeys(alice.public)))
    val transaction = aliceUnlinkData("one").toTransaction(bob.`private`)

    val result = transaction(state).left.value
    result mustBe UnableToVerifySignatureError
  }

  it should "apply an unlink" in {
    val state = IdentityLedgerState(Map("one" -> IdentityData.forKeys(daniel.public, alice.public)))
    val unlink1 = danielUnlinkData("one").toTransaction(daniel.`private`)
    val unlink2 = aliceUnlinkData("one").toTransaction(alice.`private`)
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
    val claim = aliceClaimData("one").toTransaction(alice.`private`)
    val link = bobLinkData("two").toTransaction(bob.`private`, bob.`private`)
    val unlink = bobUnlinkData("two").toTransaction(bob.`private`)
    claim.partitionIds mustBe Set(claim.data.identity)
    link.partitionIds mustBe Set(link.data.identity)
    unlink.partitionIds mustBe Set(unlink.data.identity)
  }

  it should "apply a Endorse" in {
    val state =
      IdentityLedgerState(Map("one" -> IdentityData.forKeys(alice.public), "two" -> IdentityData.forKeys(bob.public)))
    val endorseData = EndorseData("two", "one")
    val endorse = endorseData.toTransaction(bob.`private`)

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
    val endorseData = EndorseData("two", "one")
    val transaction = endorseData.toTransaction(bob.`private`)

    val result = transaction(state).left.value
    result mustBe UnknownEndorsedIdentityError("one")
  }

  it should "fail to endorse identity if the endorser identity is not claimed" in {
    val state =
      IdentityLedgerState(Map("two" -> IdentityData.forKeys(bob.public)))
    val endorseData = EndorseData("three", "two")
    val transaction = endorseData.toTransaction(bob.`private`)

    val result = transaction(state).left.value
    result mustBe UnknownEndorserIdentityError("three")
  }

  it should "fail to endorse identity if the endorser signature is not valid" in {
    val state =
      IdentityLedgerState(Map("one" -> IdentityData.forKeys(alice.public), "two" -> IdentityData.forKeys(bob.public)))
    val endorseData = EndorseData("two", "one")
    val transaction = endorseData.toTransaction(alice.`private`)

    val result = transaction(state).left.value
    result mustBe UnableToVerifyEndorserSignatureError("two", transaction.signature)
  }

  it should "fail to endorse identity if the endorser key is not valid" in {
    val state =
      IdentityLedgerState(Map("one" -> IdentityData.forKeys(alice.public), "two" -> IdentityData.forKeys(bob.public)))
    val endorseData = EndorseData("two", "one")
    val transaction = endorseData.toTransaction(daniel.`private`)

    val result = transaction(state).left.value
    result mustBe UnableToVerifyEndorserSignatureError("two", transaction.signature)
  }

  it should "apply a Revoke" in {
    val state =
      IdentityLedgerState(
        Map("one" -> IdentityData(Set(alice.public), Set("two")), "two" -> IdentityData.forKeys(bob.public)))
    val endorse = RevokeEndorsementData(
      "two",
      "one"
    ).toTransaction(bob.`private`)

    val newStateEither = endorse(state)
    val newState = newStateEither.right.value
    newState.keys mustBe Set("one", "two")
    newState.get("one").value.endorsers.size mustBe 0
    newState.get("two").value.endorsers.size mustBe 0
  }

  it should "fail to revoke a endorse identity if the identity is not claimed" in {
    val state =
      IdentityLedgerState(Map("two" -> IdentityData.forKeys(bob.public)))
    val transaction = RevokeEndorsementData(
      "two",
      "one"
    ).toTransaction(bob.`private`)

    val result = transaction(state).left.value
    result mustBe UnknownEndorsedIdentityError("one")
  }

  it should "fail to revoke a identity if the endorser identity is not claimed" in {
    val state =
      IdentityLedgerState(Map("two" -> IdentityData.forKeys(bob.public)))
    val transaction = EndorseData(
      "three",
      "two"
    ).toTransaction(bob.`private`)

    val result = transaction(state).left.value
    result mustBe UnknownEndorserIdentityError("three")
  }

  it should "fail to revoke identity if the endorser signature is not valid" in {
    val state =
      IdentityLedgerState(Map("one" -> IdentityData.forKeys(alice.public), "two" -> IdentityData.forKeys(bob.public)))
    val transaction = RevokeEndorsementData("two", "one").toTransaction(alice.`private`)

    val result = transaction(state).left.value
    result mustBe UnableToVerifyEndorserSignatureError("two", transaction.signature)
  }

  it should "fail to revoke identity if the endorser has not endorsed this identity" in {
    val state =
      IdentityLedgerState(
        Map("one" -> IdentityData(Set(alice.public), Set("three")), "two" -> IdentityData.forKeys(bob.public)))
    val transaction = RevokeEndorsementData("two", "one").toTransaction(bob.`private`)
    val result = transaction(state).left.value
    result mustBe EndorsementNotAssociatedWithIdentityError("two", "one")

  }

  it should "apply a LinkCertificate" in {
    val pem = twoChainedCertsPEM
    val pair = CachedCertificatePair.decode(pem).value
    val keys = generateSigningKeyPair()
    val state = IdentityLedgerState(
      Map(
        pair.target.identity -> IdentityData.forKeys(keys.public),
        pair.issuer.identity -> IdentityData.forKeys(pair.issuer.publicKey)))

    val certificateKey = toSigningPrivateKey(validCertPrivateKey).value
    val data = LinkCertificateData(pair.target.identity, pem)
    val tx = data.toTransaction(keys.`private`, certificateKey)

    val result = tx.apply(state).right.value

    val newData = result.get(pair.target.identity).value
    newData.endorsers must contain(pair.issuer.identity)
    newData.keys must contain(pair.target.publicKey)
  }

  it should "fail to apply a LinkCertificate when the linkingIdentity doesn't match the one from the certificate" in {
    val pem = twoChainedCertsPEM
    val pair = CachedCertificatePair.decode(pem).value
    val keys = generateSigningKeyPair()
    val state = IdentityLedgerState(
      Map(
        pair.target.identity -> IdentityData.forKeys(keys.public),
        pair.issuer.identity -> IdentityData.forKeys(pair.issuer.publicKey)))

    val certificateKey = toSigningPrivateKey(validCertPrivateKey).value
    val linkingIdentity = pair.target.identity + "XXX"
    val data = LinkCertificateData(linkingIdentity, pem)
    val tx = data.toTransaction(keys.`private`, certificateKey)

    val result = tx.apply(state).left.value

    result must be(IdentityNotMatchingCertificate(linkingIdentity, pair.target.identity))
  }

  it should "fail to apply a LinkCertificate when the authority is not registered" in {
    val pem = twoChainedCertsPEM
    val pair = CachedCertificatePair.decode(pem).value
    val keys = generateSigningKeyPair()
    val state = IdentityLedgerState(Map(pair.target.identity -> IdentityData.forKeys(keys.public)))

    val certificateKey = toSigningPrivateKey(validCertPrivateKey).value
    val data = LinkCertificateData(pair.target.identity, pem)
    val tx = data.toTransaction(keys.`private`, certificateKey)

    val result = tx.apply(state).left.value

    result must be(IdentityNotClaimedError(pair.issuer.identity))
  }

  it should "fail to apply a LinkCertificate when the linking identity is not registered" in {
    val pem = twoChainedCertsPEM
    val pair = CachedCertificatePair.decode(pem).value
    val keys = generateSigningKeyPair()
    val state = IdentityLedgerState(Map(pair.issuer.identity -> IdentityData.forKeys(pair.issuer.publicKey)))

    val certificateKey = toSigningPrivateKey(validCertPrivateKey).value
    val data = LinkCertificateData(pair.target.identity, pem)
    val tx = data.toTransaction(keys.`private`, certificateKey)

    val result = tx.apply(state).left.value

    result must be(IdentityNotClaimedError(pair.target.identity))
  }

  it should "fail to apply a LinkCertificate when authority key is not registered" in {
    val pem = twoChainedCertsPEM
    val pair = CachedCertificatePair.decode(pem).value
    val keys = generateSigningKeyPair()
    val state = IdentityLedgerState(
      Map(pair.target.identity -> IdentityData.forKeys(keys.public), pair.issuer.identity -> IdentityData.empty))

    val certificateKey = toSigningPrivateKey(validCertPrivateKey).value
    val data = LinkCertificateData(pair.target.identity, pem)
    val tx = data.toTransaction(keys.`private`, certificateKey)
    val result = tx.apply(state).left.value

    result must be(PublicKeyNotAssociatedWithIdentity(pair.issuer.identity, pair.issuer.publicKey))
  }

  it should "fail to apply a LinkCertificate when linking identity signs with a key that is not registered" in {
    val pem = twoChainedCertsPEM
    val pair = CachedCertificatePair.decode(pem).value
    val keys = generateSigningKeyPair()
    val state = IdentityLedgerState(
      Map(
        pair.target.identity -> IdentityData.forKeys(pair.issuer.publicKey),
        pair.issuer.identity -> IdentityData.forKeys(pair.issuer.publicKey)))

    val certificateKey = toSigningPrivateKey(validCertPrivateKey).value
    val data = LinkCertificateData(pair.target.identity, pem)
    val tx = data.toTransaction(keys.`private`, certificateKey)
    val result = tx.apply(state).left.value

    result must be(UnableToVerifySignatureError)
  }

  it should "fail to apply a LinkCertificate when linking identity doesn't sign with its certificate key" in {
    val pem = twoChainedCertsPEM
    val pair = CachedCertificatePair.decode(pem).value
    val keys = generateSigningKeyPair()
    val state = IdentityLedgerState(
      Map(
        pair.target.identity -> IdentityData.forKeys(keys.public),
        pair.issuer.identity -> IdentityData.forKeys(pair.issuer.publicKey)))

    val data = LinkCertificateData(pair.target.identity, pem)
    val tx = data.toTransaction(keys.`private`, keys.`private`)
    val result = tx.apply(state).left.value

    result must be(UnableToVerifyLinkingIdentitySignatureError(pair.target.identity, pair.target.publicKey))
  }

  it should "fail to apply a LinkCertificate when the pem authority didn't signed the certificate" in {
    val pem = twoUnchainedCertsPEM
    val pair = CachedCertificatePair.decode(pem).value
    val keys = generateSigningKeyPair()
    val state = IdentityLedgerState(
      Map(
        pair.target.identity -> IdentityData.forKeys(keys.public),
        pair.issuer.identity -> IdentityData.forKeys(pair.issuer.publicKey)))

    val certificateKey = toSigningPrivateKey(validCertPrivateKey).value
    val data = LinkCertificateData(pair.target.identity, pem)
    val tx = data.toTransaction(keys.`private`, certificateKey)

    val result = tx.apply(state).left.value

    result must be(UnableToVerifyLinkingIdentitySignatureError(pair.issuer.identity, pair.issuer.publicKey))
  }

  it should "fail to apply a LinkCertificate when the pem isn't valid" in {
    val pem = "xxx"
    val keys = generateSigningKeyPair()
    val state = IdentityLedgerState(Map.empty)
    val pair = CachedCertificatePair.decode(twoUnchainedCertsPEM).value

    val certificateKey = toSigningPrivateKey(validCertPrivateKey).value
    val data = LinkCertificateData(pair.target.identity, pem)
    val tx = data.toTransaction(keys.`private`, certificateKey)
    val result = tx.apply(state).left.value

    result must be(InvalidCertificateError)
  }
}
