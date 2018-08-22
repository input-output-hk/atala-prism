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

import scala.collection.immutable
import scala.util.Try

trait ChimericLedgerItDbTest extends fixture.FlatSpec
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
    val value3 = Value(Map(currency1 -> BigDecimal(2)))
    val singleFee = Value(Map(currency1 -> BigDecimal(1)))
    val multiFee = Value(Map(currency1 -> BigDecimal(1), currency2 -> BigDecimal(2)))
    val ledger = createLedger(stateStorage)
    val utxoTx = ChimericTx(Seq(
      Withdrawal(address1, value3, 1),
      Output(value3 - singleFee),
      Fee(singleFee)
    ))
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
        Deposit(address2, value1 - multiFee),
        Fee(multiFee)
      )),
      utxoTx
    )
    val header = new ChimericBlockHeader
    val block = Block(header, transactions)
    val result = ledger(block)
    result.isRight mustBe true
    result.right.get.isSuccess mustBe true

    val address1Key = ChimericLedgerState.getAddressPartitionId(address1)
    val address2Key = ChimericLedgerState.getAddressPartitionId(address2)
    val currency1Key = ChimericLedgerState.getCurrencyPartitionId(currency1)
    val currency2Key = ChimericLedgerState.getCurrencyPartitionId(currency2)
    val utxoKey = ChimericLedgerState.getUtxoPartitionId(TxOutRef(utxoTx.txId, 1))
    val allKeys = Set(address1Key, address2Key, currency1Key, currency2Key, utxoKey)
    stateStorage.slice(allKeys) mustBe LedgerState[ChimericStateValue](Map(
      currency1Key -> CreateCurrencyHolder(CreateCurrency(currency1)),
      currency2Key -> CreateCurrencyHolder(CreateCurrency(currency2)),
      utxoKey -> ValueHolder(value3 - singleFee),
      address1Key -> ValueHolder(value2 - value3),
      address2Key -> ValueHolder(value1 - multiFee)
    ))
    val block2 = Block(header, immutable.Seq(
      ChimericTx(immutable.Seq(
        Input(TxOutRef(utxoTx.txId, 1), value3 - singleFee),
        Fee(value3 - singleFee),
        Withdrawal(address1, value2 - value3, 1),
        Fee(value2 - value3)
      ))
    ))
    val result2 = ledger(block2)
    result2.isRight mustBe true
    result2.right.get.isSuccess mustBe true
    stateStorage.slice(allKeys) mustBe LedgerState[ChimericStateValue](Map(
      currency1Key -> CreateCurrencyHolder(CreateCurrency(currency1)),
      currency2Key -> CreateCurrencyHolder(CreateCurrency(currency2)),
      address2Key -> ValueHolder(value1 - multiFee)
    ))
  }
}
