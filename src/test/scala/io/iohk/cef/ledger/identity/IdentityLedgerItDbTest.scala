package io.iohk.cef.ledger.identity

import java.security.PublicKey
import java.time.{Clock, Instant}

import akka.util.ByteString
import io.iohk.cef.builder.RSAKeyGenerator
import io.iohk.cef.crypto.low.DigitalSignature
import io.iohk.cef.ledger.Block
import io.iohk.cef.ledger.identity.IdentityBlockSerializer._
import io.iohk.cef.ledger.identity.storage.scalike.IdentityLedgerStateStorageImpl
import io.iohk.cef.ledger.identity.storage.scalike.dao.IdentityLedgerStateStorageDao
import io.iohk.cef.ledger.storage.Ledger
import io.iohk.cef.ledger.storage.scalike.LedgerStorageImpl
import io.iohk.cef.ledger.storage.scalike.dao.LedgerStorageDao
import io.iohk.cef.utils.ForExpressionsEnabler
import org.scalatest.{EitherValues, MustMatchers, fixture}
import scalikejdbc._
import scalikejdbc.scalatest.AutoRollback

import scala.util.Try

trait IdentityLedgerItDbTest
    extends fixture.FlatSpec
    with AutoRollback
    with MustMatchers
    with RSAKeyGenerator
    with EitherValues
    with IdentityLedgerStateStorageFixture {

  val dummySignature = new DigitalSignature(ByteString.empty)

  def createLedger(ledgerStateStorageDao: IdentityLedgerStateStorageDao)(implicit dBSession: DBSession): Ledger[Try, Set[PublicKey]] = {
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
    val pair1 = generateKeyPair
    val pair2 = generateKeyPair
    val pair3 = generateKeyPair

    val ledgerStateStorageDao = new IdentityLedgerStateStorageDao
    val ledger = createLedger(ledgerStateStorageDao)
    val now = Instant.now()
    val header = IdentityBlockHeader(ByteString("header"), now, 1)
    val block1 = Block(header, List[IdentityTransaction](
      Claim("one", pair1._1, dummySignature),
      Claim("two", pair2._1, dummySignature)))

    val block1Result = ledger(block1)
    block1Result.right.value.isSuccess mustBe true

    ledgerStateStorageDao.slice(Set("one")) mustBe IdentityLedgerState(Map("one" -> Set(pair1._1)))
    ledgerStateStorageDao.slice(Set("two")) mustBe IdentityLedgerState(Map("two" -> Set(pair2._1)))
    ledgerStateStorageDao.slice(Set("three")) mustBe IdentityLedgerState()
    ledgerStateStorageDao.slice(Set("one","two","three")) mustBe
      IdentityLedgerState(Map(
        "one" -> Set(pair1._1),
        "two" -> Set(pair2._1)))

    val block2 = Block(header.copy(height = 2), List[IdentityTransaction](
      Link("two", pair3._1, IdentityTransaction.sign("two", pair3._1, pair2._2))))

    val block2Result = ledger(block2)
    block2Result.right.value.isSuccess mustBe true

    ledgerStateStorageDao.slice(Set("one","two")) mustBe
      IdentityLedgerState(
        Map(
          "one" -> Set(pair1._1),
          "two" -> Set(pair2._1, pair3._1))
      )

    val block3 = Block(header.copy(height = 2), List[IdentityTransaction](
      Link("three", pair3._1, IdentityTransaction.sign("three", pair3._1, pair2._2))))
    val invalidResult = ledger(block3)

    if (invalidResult.isRight) {
      fail(s"Link transaction should've been invalid. Expected IdentityNotClaimedError but got ${invalidResult}")
    }
  }
}
