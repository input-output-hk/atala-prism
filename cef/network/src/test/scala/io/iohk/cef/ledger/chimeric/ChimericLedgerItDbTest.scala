package io.iohk.cef.ledger.chimeric

import java.time.Clock

import io.iohk.cef.ledger.chimeric.ChimericBlockSerializer._
import io.iohk.cef.ledger.chimeric.storage.scalike.ChimericLedgerStateStorageImpl
import io.iohk.cef.ledger.chimeric.storage.scalike.dao.ChimericLedgerStateStorageDao
import io.iohk.cef.ledger.storage.Ledger
import io.iohk.cef.ledger.storage.scalike.LedgerStorageImpl
import io.iohk.cef.ledger.storage.scalike.dao.LedgerStorageDao
import io.iohk.cef.ledger.{Block, LedgerFixture, LedgerState}
import org.scalatest.{MustMatchers, fixture}
import scalikejdbc._
import scalikejdbc.scalatest.AutoRollback

import scala.util.Try

class ChimericLedgerItDbTest extends fixture.FlatSpec
  with AutoRollback
  with MustMatchers
  with LedgerFixture {

  def createLedger(ledgerStateStorageDao: ChimericLedgerStateStorageDao)(implicit dBSession: DBSession): Ledger[Try, ChimericStateValue] = {
    val ledgerStateStorage = new ChimericLedgerStateStorageImpl(ledgerStateStorageDao) {
      override def execInSession[T](block: DBSession => T): T = block(dBSession)
    }
    val ledgerStorageDao = new LedgerStorageDao(Clock.systemUTC())
    val ledgerStorage = new LedgerStorageImpl(ledgerStorageDao) {
      override def execInSession[T](block: DBSession => T): T = block(dBSession)
    }
    createLedger(ledgerStateStorage, ledgerStorage)
  }

  behavior of "ChimericLedger"

  it should "store transactions" in { implicit s =>
    val stateStorage = new ChimericLedgerStateStorageDao
    val address1 = "address1"
    val address2 = "address2"
    val currency1 = "currency1"
    val currency2 = "currency2"
    val value1 = Value(Map(currency1 -> BigDecimal(10), currency2 -> BigDecimal(20)))
    val value2 = Value(Map(currency1 -> BigDecimal(100), currency2 -> BigDecimal(200)))
    val fee = Value(Map(currency1 -> BigDecimal(1), currency2 -> BigDecimal(2)))
    val ledger = createLedger(stateStorage)
    val transactions = List[ChimericTx](
      ChimericTx(Seq(
        CreateCurrency(currency1),
        CreateCurrency(currency2),
        Mint(value1),
        Mint(value2),
        Deposit(address1, value1 + value2)
      )),
      ChimericTx(Seq(
        Withdrawal(address1, value1, 1),
        Deposit(address2, value1 - fee),
        Fee(fee)
      ))
    )
    val header = new ChimericBlockHeader {}
    val block = Block(header, transactions)
    val result = ledger(block)
    result.isRight mustBe true
    result.right.get.isSuccess mustBe true

    val address1Key = ChimericLedgerState.getAddressPartitionId(address1)
    val address2Key = ChimericLedgerState.getAddressPartitionId(address2)
    stateStorage.slice(Set(address1Key)) mustBe LedgerState[ChimericStateValue](Map(
      address1Key -> ValueHolder(value2),
      address2Key -> ValueHolder(value1 - fee)
    ))
  }
}
