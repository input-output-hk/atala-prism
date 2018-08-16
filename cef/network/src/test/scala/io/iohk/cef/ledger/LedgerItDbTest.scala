package io.iohk.cef.ledger

import java.time.{Clock, Instant}

import akka.util.ByteString
import io.iohk.cef.ledger.identity.{Claim, IdentityBlockHeader, IdentityTransaction, Link}
import io.iohk.cef.ledger.storage.Ledger
import io.iohk.cef.ledger.storage.scalike.dao.{LedgerStateStorageDao, LedgerStorageDao}
import io.iohk.cef.ledger.storage.scalike.{LedgerStateStorageImpl, LedgerStorageImpl}
import org.scalatest.{MustMatchers, fixture}
import scalikejdbc.scalatest.AutoRollback

import scala.util.Success

trait LedgerItDbTest extends fixture.FlatSpec with AutoRollback with MustMatchers {
  behavior of "Ledger"

  it should "apply a block using the generic constructs" in { implicit session =>
    import identity.IdentityStateSerializer._
    import identity.IdentityBlockSerializer._
    implicit val enabler = io.iohk.cef.utils.ForExpressionsEnabler.tryEnabler
    val genericStateDao = new LedgerStateStorageDao[Set[ByteString]]()
    val genericLedgerDao = new LedgerStorageDao(Clock.systemUTC())
    val genericStateImpl = new LedgerStateStorageImpl(1, genericStateDao) {
      override protected def execInSession[T](block: FixtureParam => T): T = block(session)
    }
    val genericLedgerStorageImpl = new LedgerStorageImpl(genericLedgerDao) {
      override protected def execInSession[T](block: FixtureParam => T): T = block(session)
    }
    val ledger = new Ledger(1, genericLedgerStorageImpl, genericStateImpl)

    val testTxs = List[IdentityTransaction](Claim("carlos", ByteString("key1")), Link("carlos", ByteString("key2")))
    val testBlock = Block(IdentityBlockHeader(ByteString("hash"), Instant.EPOCH, 1L), testTxs)
    val emptyLs = LedgerState[Set[ByteString]](Map())
    genericStateDao.slice(1, Set("carlos")) mustBe emptyLs
    ledger(testBlock) mustBe Right(Success(()))
    genericStateDao.slice(1, Set("carlos")) mustBe
      LedgerState[Set[ByteString]](Map("carlos" -> Set(ByteString("key1"), ByteString("key2"))))
  }
}
