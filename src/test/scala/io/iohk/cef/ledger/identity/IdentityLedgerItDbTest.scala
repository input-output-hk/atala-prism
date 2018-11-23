package io.iohk.cef.ledger.identity

import java.time.{Clock, Instant}

import io.iohk.cef.builder.SigningKeyPairs
import io.iohk.cef.ledger.storage.scalike.LedgerStateStorageImpl
import io.iohk.cef.ledger.storage.scalike.dao.LedgerStateStorageDao
import io.iohk.cef.frontend.models.IdentityTransactionType
import io.iohk.cef.ledger.{Block, BlockHeader, LedgerState}
import io.iohk.cef.ledger.storage.Ledger
import io.iohk.cef.ledger.storage.scalike.LedgerStorageImpl
import io.iohk.cef.ledger.storage.scalike.dao.LedgerStorageDao
import org.scalatest.{EitherValues, MustMatchers, fixture}
import scalikejdbc._
import scalikejdbc.scalatest.AutoRollback
import io.iohk.cef.codecs.nio.auto._

trait IdentityLedgerItDbTest
    extends fixture.FlatSpec
    with AutoRollback
    with MustMatchers
    with SigningKeyPairs
    with EitherValues
    with IdentityLedgerStateStorageFixture {

  def createLedger(ledgerStateStorageDao: LedgerStateStorageDao)(implicit dBSession: DBSession): Ledger = {
    val ledgerStateStorage = new LedgerStateStorageImpl("identityLedger", ledgerStateStorageDao) {
      override def execInSession[T](block: DBSession => T): T = block(dBSession)
    }
    val ledgerStorageDao = new LedgerStorageDao(Clock.systemUTC())
    val ledgerStorage = new LedgerStorageImpl(ledgerStorageDao) {
      override def execInSession[T](block: DBSession => T): T = block(dBSession)
    }
    Ledger("1", ledgerStorage, ledgerStateStorage)
  }

  behavior of "IdentityLedgerIt"

  it should "throw an error when the tx is inconsistent with the state" in { implicit session =>
    val ledgerStateStorageDao = new LedgerStateStorageDao
    def slice(keys: Set[String]): LedgerState[IdentityData] =
      ledgerStateStorageDao.slice[IdentityData]("identityLedger", keys)

    val ledger = createLedger(ledgerStateStorageDao)
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

    ledger(block1).isRight mustBe true

    slice(Set("one")) mustBe IdentityLedgerState(Map("one" -> IdentityData.forKeys(alice.public)))
    slice(Set("two")) mustBe IdentityLedgerState(Map("two" -> IdentityData.forKeys(bob.public)))
    slice(Set("three")) mustBe IdentityLedgerState()
    slice(Set("one", "two", "three")) mustBe
      IdentityLedgerState(Map("one" -> IdentityData.forKeys(alice.public), "two" -> IdentityData.forKeys(bob.public)))

    val block2 = Block[IdentityData, IdentityTransaction](
      header,
      List[IdentityTransaction](
        Link(
          "two",
          carlos.public,
          IdentityTransaction.sign("two", IdentityTransactionType.Link, carlos.public, bob.`private`))))

    ledger(block2).isRight mustBe true

    slice(Set("one", "two")) mustBe
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
