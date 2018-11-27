package io.iohk.cef.ledger.identity

import java.nio.file.Files
import java.time.Instant

import io.iohk.cef.builder.SigningKeyPairs
import io.iohk.cef.frontend.models.IdentityTransactionType
import io.iohk.cef.ledger.{Block, BlockHeader}
import io.iohk.cef.ledger.storage.Ledger
import org.scalatest.{EitherValues, MustMatchers, fixture}
import scalikejdbc.scalatest.AutoRollback
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.ledger.storage.mv.{MVLedgerStateStorage, MVLedgerStorage}

trait IdentityLedgerItDbTest
    extends fixture.FlatSpec
    with AutoRollback
    with MustMatchers
    with SigningKeyPairs
    with EitherValues
    with IdentityLedgerStateStorageFixture {

  def createLedger(): Ledger[IdentityData, IdentityTransaction] = {
    val ledgerStateStorage =
      new MVLedgerStateStorage[IdentityData]("identityLedger", Files.createTempFile("", "").toAbsolutePath)
    val ledgerStorage = new MVLedgerStorage[IdentityData, IdentityTransaction](
      "identityLedger",
      Files.createTempFile("", "").toAbsolutePath)
    Ledger("1", ledgerStorage, ledgerStateStorage)
  }

  behavior of "IdentityLedgerIt"

  it should "throw an error when the tx is inconsistent with the state" in { implicit session =>
    val ledger: Ledger[IdentityData, IdentityTransaction] = createLedger()
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
