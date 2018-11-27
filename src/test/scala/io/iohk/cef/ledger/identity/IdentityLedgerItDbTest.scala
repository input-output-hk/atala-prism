package io.iohk.cef.ledger.identity

import java.time.Instant

import io.iohk.cef.DatabaseTestSuites.withLedger
import io.iohk.cef.frontend.models.IdentityTransactionType
import io.iohk.cef.ledger.{Block, BlockHeader}
import io.iohk.cef.codecs.nio.auto._
import org.scalatest.FlatSpec
import org.scalatest.MustMatchers._
import io.iohk.cef.builder.SigningKeyPairs._

class IdentityLedgerItDbTest extends FlatSpec {

  behavior of "IdentityLedgerIt"

  it should "error when the tx is inconsistent with the state" in withLedger[IdentityData, IdentityTransaction](
    "identityLedger") { ledger =>
    val now = Instant.now()
    val header = BlockHeader(now)
    val block1 = Block[IdentityData, IdentityTransaction](
      header,
      List[IdentityTransaction](
        Claim(
          "one",
          alice.public,
          IdentityTransaction.sign("one", IdentityTransactionType.Claim, alice.public, alice.`private`)),
        Claim(
          "two",
          bob.public,
          IdentityTransaction.sign("two", IdentityTransactionType.Claim, bob.public, bob.`private`))
      )
    )

    ledger.apply(block1).isRight mustBe true

    ledger.slice(Set("one")) mustBe IdentityLedgerState(Map("one" -> IdentityData.forKeys(alice.public)))
    ledger.slice(Set("two")) mustBe IdentityLedgerState(Map("two" -> IdentityData.forKeys(bob.public)))
    ledger.slice(Set("three")) mustBe IdentityLedgerState()
    ledger.slice(Set("one", "two", "three")) mustBe
      IdentityLedgerState(Map("one" -> IdentityData.forKeys(alice.public), "two" -> IdentityData.forKeys(bob.public)))

    val block2 = Block[IdentityData, IdentityTransaction](
      header,
      List[IdentityTransaction](
        Link(
          "two",
          carlos.public,
          IdentityTransaction.sign("two", IdentityTransactionType.Link, carlos.public, bob.`private`))))

    ledger(block2).isRight mustBe true

    ledger.slice(Set("one", "two")) mustBe
      IdentityLedgerState(
        Map("one" -> IdentityData.forKeys(alice.public), "two" -> IdentityData.forKeys(bob.public, carlos.public))
      )

    val block3 = Block[IdentityData, IdentityTransaction](
      header,
      List[IdentityTransaction](
        Link(
          "three",
          carlos.public,
          IdentityTransaction.sign("three", IdentityTransactionType.Link, carlos.public, bob.`private`)))
    )
    val invalidResult = ledger(block3)

    if (invalidResult.isRight) {
      fail(s"Link transaction should've been invalid. Expected IdentityNotClaimedError but got ${invalidResult}")
    }
  }
}
