package io.iohk.cef.ledger.identity

import java.time.{Clock, Instant}

import io.iohk.cef.builder.SigningKeyPairs
import io.iohk.cef.crypto._
import io.iohk.cef.ledger.Block
import io.iohk.cef.ledger.identity.IdentityBlockSerializer._
import io.iohk.cef.ledger.identity.storage.scalike.IdentityLedgerStateStorageImpl
import io.iohk.cef.ledger.identity.storage.scalike.dao.IdentityLedgerStateStorageDao
import io.iohk.cef.ledger.storage.Ledger
import io.iohk.cef.ledger.storage.scalike.LedgerStorageImpl
import io.iohk.cef.ledger.storage.scalike.dao.LedgerStorageDao
import org.scalatest.{EitherValues, MustMatchers, fixture}
import scalikejdbc._
import scalikejdbc.scalatest.AutoRollback

trait IdentityLedgerItDbTest
    extends fixture.FlatSpec
    with AutoRollback
    with MustMatchers
    with SigningKeyPairs
    with EitherValues
    with IdentityLedgerStateStorageFixture {

  def createLedger(ledgerStateStorageDao: IdentityLedgerStateStorageDao)(
      implicit dBSession: DBSession): Ledger[Set[SigningPublicKey]] = {
    val ledgerStateStorage = new IdentityLedgerStateStorageImpl(ledgerStateStorageDao) {
      override def execInSession[T](block: DBSession => T): T = block(dBSession)
    }
    val ledgerStorageDao = new LedgerStorageDao(Clock.systemUTC())
    val ledgerStorage = new LedgerStorageImpl(ledgerStorageDao) {
      override def execInSession[T](block: DBSession => T): T = block(dBSession)
    }
    Ledger(1, ledgerStorage, ledgerStateStorage)
  }

  behavior of "IdentityLedgerIt"

  it should "throw an error when the tx is inconsistent with the state" in { implicit session =>
    val ledgerStateStorageDao = new IdentityLedgerStateStorageDao
    val ledger = createLedger(ledgerStateStorageDao)
    val now = Instant.now()
    val header = IdentityBlockHeader(now)
    val block1 = Block(
      header,
      List[IdentityTransaction](
        Claim("one", alice.public, IdentityTransaction.sign("one", alice.public, alice.`private`)),
        Claim("two", bob.public, IdentityTransaction.sign("two", bob.public, bob.`private`))
      )
    )

    ledger(block1).isRight mustBe true

    ledgerStateStorageDao.slice(Set("one")) mustBe IdentityLedgerState(Map("one" -> Set(alice.public)))
    ledgerStateStorageDao.slice(Set("two")) mustBe IdentityLedgerState(Map("two" -> Set(bob.public)))
    ledgerStateStorageDao.slice(Set("three")) mustBe IdentityLedgerState()
    ledgerStateStorageDao.slice(Set("one", "two", "three")) mustBe
      IdentityLedgerState(Map("one" -> Set(alice.public), "two" -> Set(bob.public)))

    val block2 = Block(
      header,
      List[IdentityTransaction](
        Link("two", carlos.public, IdentityTransaction.sign("two", carlos.public, bob.`private`))))

    ledger(block2).isRight mustBe true

    ledgerStateStorageDao.slice(Set("one", "two")) mustBe
      IdentityLedgerState(
        Map("one" -> Set(alice.public), "two" -> Set(bob.public, carlos.public))
      )

    val block3 = Block(
      header,
      List[IdentityTransaction](
        Link("three", carlos.public, IdentityTransaction.sign("three", carlos.public, bob.`private`))))
    val invalidResult = ledger(block3)

    if (invalidResult.isRight) {
      fail(s"Link transaction should've been invalid. Expected IdentityNotClaimedError but got ${invalidResult}")
    }
  }
}
