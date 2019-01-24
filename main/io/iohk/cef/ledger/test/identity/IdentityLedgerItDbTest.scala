package io.iohk.cef.ledger.identity

import java.time.Instant

import io.iohk.cef.test.DatabaseTestSuites.withLedger
import io.iohk.cef.test.builder.SigningKeyPairs._
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.ledger.{Block, BlockHeader}
import org.scalatest.FlatSpec
import org.scalatest.MustMatchers._

class IdentityLedgerItDbTest extends FlatSpec {

  behavior of "IdentityLedgerIt"

  it should "error when the tx is inconsistent with the state" in withLedger[IdentityData, IdentityTransaction](
    "identityLedger"
  ) { ledger =>
    val now = Instant.now()
    val header = BlockHeader(now)
    val firstClaimData = ClaimData("one", alice.public)
    val secondClaimData = ClaimData("two", bob.public)
    val linkData = LinkData("two", carlos.public)
    val secondLinkData = LinkData("three", carlos.public)
    val block1 = Block[IdentityData, IdentityTransaction](
      header,
      List[IdentityTransaction](
        Claim(firstClaimData, alice.`private`),
        Claim(secondClaimData, bob.`private`)
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
      List[IdentityTransaction](Link(linkData, bob.`private`, carlos.`private`))
    )

    val x = ledger(block2)

    x.isRight mustBe true

    ledger.slice(Set("one", "two")) mustBe
      IdentityLedgerState(
        Map("one" -> IdentityData.forKeys(alice.public), "two" -> IdentityData.forKeys(bob.public, carlos.public))
      )

    val block3 = Block[IdentityData, IdentityTransaction](
      header,
      List[IdentityTransaction](Link(secondLinkData, bob.`private`, carlos.`private`))
    )
    val invalidResult = ledger(block3)

    if (invalidResult.isRight) {
      fail(s"Link transaction should've been invalid. Expected IdentityNotClaimedError but got ${invalidResult}")
    }
  }
}
