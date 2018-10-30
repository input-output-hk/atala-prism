package io.iohk.cef.ledger

import java.time.{Clock, Instant}

import io.iohk.cef.builder.SigningKeyPairs
import io.iohk.cef.crypto._
import io.iohk.cef.frontend.models.IdentityTransactionType
import io.iohk.cef.ledger.identity._
import io.iohk.cef.ledger.storage.Ledger
import io.iohk.cef.ledger.storage.scalike.dao.{LedgerStateStorageDao, LedgerStorageDao}
import io.iohk.cef.ledger.storage.scalike.{LedgerStateStorageImpl, LedgerStorageImpl}
import org.scalatest.{MustMatchers, fixture}
import scalikejdbc.scalatest.AutoRollback

trait LedgerItDbTest extends fixture.FlatSpec with AutoRollback with SigningKeyPairs with MustMatchers {

  import IdentityBlockSerializer._
  import IdentityStateSerializer._

  behavior of "Ledger"

  it should "apply a block using the generic constructs" in { implicit session =>
    val genericStateDao = new LedgerStateStorageDao[Set[SigningPublicKey]]()
    val genericLedgerDao = new LedgerStorageDao(Clock.systemUTC())
    val genericStateImpl = new LedgerStateStorageImpl("1", genericStateDao) {
      override protected def execInSession[T](block: FixtureParam => T): T = block(session)
    }
    val genericLedgerStorageImpl = new LedgerStorageImpl(genericLedgerDao) {
      override protected def execInSession[T](block: FixtureParam => T): T = block(session)
    }
    val ledger = Ledger("1", genericLedgerStorageImpl, genericStateImpl)

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
    val testBlock = Block(IdentityBlockHeader(Instant.EPOCH), testTxs)
    val emptyLs = LedgerState[Set[SigningPublicKey]](Map())
    genericStateDao.slice("1", Set("carlos")) mustBe emptyLs

    ledger(testBlock) mustBe Right(())
    genericStateDao.slice("1", Set("carlos")) mustBe
      LedgerState[Set[SigningPublicKey]](Map("carlos" -> Set(alice.public, bob.public)))
  }
}
