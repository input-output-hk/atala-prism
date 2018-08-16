package io.iohk.cef.ledger.identity

import java.time.{Clock, Instant}

import akka.util.ByteString
import io.iohk.cef.ledger.Block
import io.iohk.cef.ledger.identity.IdentityBlockSerializer._
import io.iohk.cef.ledger.identity.storage.scalike.IdentityLedgerStateStorageImpl
import io.iohk.cef.ledger.identity.storage.scalike.dao.IdentityLedgerStateStorageDao
import io.iohk.cef.ledger.storage.Ledger
import io.iohk.cef.ledger.storage.scalike.LedgerStorageImpl
import io.iohk.cef.ledger.storage.scalike.dao.LedgerStorageDao
import io.iohk.cef.utils.ForExpressionsEnabler
import org.scalatest.{MustMatchers, fixture}
import scalikejdbc._
import scalikejdbc.scalatest.AutoRollback

import scala.util.Try

trait IdentityLedgerItDbTest extends fixture.FlatSpec
  with AutoRollback
  with MustMatchers
  with IdentityLedgerStateStorageFixture {

  def createLedger(ledgerStateStorageDao: IdentityLedgerStateStorageDao)(implicit dBSession: DBSession): Ledger[Try, Set[ByteString]] = {
    implicit val forExpEnabler = ForExpressionsEnabler.tryEnabler
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
    val header = IdentityBlockHeader(ByteString("header"), now, 1)
    val block1 = Block(header, List[IdentityTransaction](Claim("one", ByteString("one")), Claim("two", ByteString("two"))))

    val block1Result = ledger(block1)
    block1Result.isRight mustBe true
    block1Result.right.get.isSuccess mustBe true

    ledgerStateStorageDao.slice(Set("one")) mustBe IdentityLedgerState(Map("one" -> Set(ByteString("one"))))
    ledgerStateStorageDao.slice(Set("two")) mustBe IdentityLedgerState(Map("two" -> Set(ByteString("two"))))
    ledgerStateStorageDao.slice(Set("three")) mustBe IdentityLedgerState()
    ledgerStateStorageDao.slice(Set("one","two","three")) mustBe
      IdentityLedgerState(Map("one" -> Set(ByteString("one")), "two" -> Set(ByteString("two"))))
    val block2 = Block(header.copy(height = 2), List[IdentityTransaction](Link("two", ByteString("two-two"))))

    val block2Result = ledger(block2)
    block2Result.isRight mustBe true
    block2Result.right.get.isSuccess mustBe true

    ledgerStateStorageDao.slice(Set("one","two")) mustBe
      IdentityLedgerState(
        Map("one" -> Set(ByteString("one")), "two" -> Set(ByteString("two"), ByteString("two-two")))
      )

    val block3 = Block(header.copy(height = 2), List[IdentityTransaction](Link("three", ByteString("three"))))
    val invalidResult = ledger(block3)

    if(invalidResult.isRight) {
      fail(s"Link transaction should've been invalid. Expected IdentityNotClaimedError but got ${invalidResult}")
    }
  }
}
