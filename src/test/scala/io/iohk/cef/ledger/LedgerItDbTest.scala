package io.iohk.cef.ledger

import java.time.Instant

import io.iohk.cef.DatabaseTestSuites
import io.iohk.cef.builder.SigningKeyPairs._
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.ledger.identity._
import org.scalatest.FlatSpec
import org.scalatest.MustMatchers._

class LedgerItDbTest extends FlatSpec {

  behavior of "Ledger"

  it should "apply a block using the generic constructs" in DatabaseTestSuites
    .withLedger[IdentityData, IdentityTransaction]("1") { ledger =>
      val claimData = ClaimData("carlos", alice.public)
      val linkData = LinkData("carlos", bob.public)
      val testTxs =
        List[IdentityTransaction](
          claimData.toTransaction(alice.`private`),
          linkData.toTransaction(alice.`private`, bob.`private`))
      val testBlock = Block[IdentityData, IdentityTransaction](BlockHeader(Instant.EPOCH), testTxs)
      ledger.slice(Set("carlos")) mustBe LedgerState()

      ledger(testBlock) mustBe Right(())
      ledger.slice(Set("carlos")) mustBe
        LedgerState[IdentityData](Map("carlos" -> IdentityData.forKeys(alice.public, bob.public)))
    }
}
