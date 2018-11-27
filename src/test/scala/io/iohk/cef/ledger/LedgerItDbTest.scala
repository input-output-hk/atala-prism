package io.iohk.cef.ledger

import java.nio.file.Files
import java.time.Instant

import io.iohk.cef.builder.SigningKeyPairs
import io.iohk.cef.frontend.models.IdentityTransactionType
import io.iohk.cef.ledger.identity._
import io.iohk.cef.ledger.storage.Ledger
import org.scalatest.{MustMatchers, fixture}
import scalikejdbc.scalatest.AutoRollback
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.codecs.nio.CodecTestingHelpers
import io.iohk.cef.ledger.storage.mv.{MVLedgerStateStorage, MVLedgerStorage}

trait LedgerItDbTest
    extends fixture.FlatSpec
    with AutoRollback
    with SigningKeyPairs
    with MustMatchers
    with CodecTestingHelpers {

  behavior of "Ledger"

  it should "apply a block using the generic constructs" in { implicit session =>
    pending
    val genericStateImpl =
      new MVLedgerStateStorage[IdentityData]("1", Files.createTempFile("", "").toAbsolutePath)
    val genericLedgerStorageImpl =
      new MVLedgerStorage[IdentityData, IdentityTransaction]("1", Files.createTempFile("", "").toAbsolutePath)

    val ledger = Ledger[IdentityData, IdentityTransaction]("1", genericLedgerStorageImpl, genericStateImpl)

    val testTxs = List[IdentityTransaction](
      Claim(
        "carlos",
        alice.public,
        IdentityTransaction.sign("carlos", IdentityTransactionType.Claim, alice.public, alice.`private`)),
      Link(
        "carlos",
        bob.public,
        IdentityTransaction.sign("carlos", IdentityTransactionType.Link, bob.public, alice.`private`))
    )
    val testBlock = Block[IdentityData, IdentityTransaction](BlockHeader(Instant.EPOCH), testTxs)
    ledger.slice(Set("carlos")) mustBe LedgerState()

    ledger(testBlock) mustBe Right(())
    ledger.slice(Set("carlos")) mustBe
      LedgerState[IdentityData](Map("carlos" -> IdentityData.forKeys(alice.public, bob.public)))
  }
}
