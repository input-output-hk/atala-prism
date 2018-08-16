package io.iohk.cef.ledger.chimeric.storage.scalike.dao

import io.iohk.cef.ledger.LedgerState
import io.iohk.cef.ledger.chimeric._
import io.iohk.cef.ledger.chimeric.storage.scalike._
import org.scalatest.{MustMatchers, fixture}
import scalikejdbc.scalatest.AutoRollback
import scalikejdbc._

trait ChimericLedgerStateStorageDaoDbTest extends fixture.FlatSpec
  with AutoRollback
  with MustMatchers {

  def insertPairs(pairs: Seq[(String, ChimericStateValue)])(implicit DBSession: DBSession): Unit = {
    val column = ChimericLedgerStateEntryTable.column
    val se = ChimericLedgerStateEntryTable.syntax("se")
    pairs.foreach(item => {
      val (key, value) = item
      sql"""
            insert into ${ChimericLedgerStateEntryTable.table} (${column.stringId})
              values (${key})
            """.update.apply()
      val entryId =
        sql"""
           select ${se.result.*}
           from ${ChimericLedgerStateEntryTable as se}
           where ${se.stringId} = ${key}
           """.map(ChimericLedgerStateEntryTable(se.resultName)(_)).toOption().apply().get.id
      insertKey(entryId, key)
      insertValue(entryId, value)
    })
  }

  private def insertKey(entryId: Long, key: String)(implicit DBSession: DBSession): Unit = {
    ChimericLedgerState.toStateKey(key) match {
      case AddressHolder(address) =>
        val column = ChimericLedgerStateAddressTable.column
        sql"""
             insert into ${ChimericLedgerStateAddressTable.table}
              (${column.id}, ${column.address})
              values (${entryId}, ${address})
            """.update.apply()
      case UtxoHolder(txOutRef) =>
        val column = ChimericLedgerStateUtxoTable.column
        sql"""
             insert into ${ChimericLedgerStateUtxoTable.table}
              (${column.id}, ${column.txId}, ${column.index})
              values (${entryId}, ${txOutRef.txId}, ${txOutRef.index})
            """.update.apply()
      case CurrencyHolder(currency) =>
        val column = ChimericLedgerStateCurrencyTable.column
        sql"""
             insert into ${ChimericLedgerStateCurrencyTable.table}
              (${column.id}, ${column.currency})
              values (${entryId}, ${currency})
            """.update.apply()
    }
  }

  private def insertValue(entryId: Long, value: ChimericStateValue)(implicit DBSession: DBSession): Unit = {
    value match {
      case ValueHolder(value) =>
        val column = ChimericValueEntryTable.column
        value.iterator.foreach {
          case (currency, quantity) =>
            sql"""
             insert into ${ChimericValueEntryTable.table}
              (${column.ledgerStateEntryId}, ${column.currency}, ${column.amount})
              values (${entryId}, ${currency}, ${quantity})
            """.update.apply()
        }
      case CreateCurrencyHolder(_) => ()
    }
  }

  behavior of "LedgerStateStorageDao"

  it should "retrieve a slice" in { implicit s =>
    val address = "address"
    val currency1 = "currency"
    val currency2 = "currency2"
    val addressValue = Value(Map(
      currency1 -> BigDecimal(1.5),
      currency2 -> BigDecimal(0.5)))
    val utxoref = TxOutRef("txId", 1)
    val utxorefValue = Value(Map(
      currency1 -> BigDecimal(2.99999),
      currency2 -> BigDecimal(3.5)
    ))
    val addressKey = ChimericLedgerState.getAddressPartitionId(address)
    val currency1Key = ChimericLedgerState.getCurrencyPartitionId(currency1)
    val currency2Key = ChimericLedgerState.getCurrencyPartitionId(currency2)
    val utxorefKey = ChimericLedgerState.getUtxoPartitionId(utxoref)
    val missingKey = ChimericLedgerState.getAddressPartitionId("")
    val pairs = Seq(
      addressKey -> ValueHolder(addressValue),
      utxorefKey -> ValueHolder(utxorefValue),
      currency1Key -> CreateCurrencyHolder(CreateCurrency(currency1)),
      currency2Key -> CreateCurrencyHolder(CreateCurrency(currency2))
    )
    insertPairs(pairs)
    val dao = new ChimericLedgerStateStorageDao()
    dao.slice(Set(addressKey)) mustBe LedgerState[ChimericStateValue](Map(
      addressKey -> ValueHolder(addressValue)
    ))
    dao.slice(Set(utxorefKey)) mustBe LedgerState[ChimericStateValue](Map(
      utxorefKey -> ValueHolder(utxorefValue)
    ))
    dao.slice(Set(currency1Key)) mustBe LedgerState[ChimericStateValue](Map(
      currency1Key -> CreateCurrencyHolder(CreateCurrency(currency1))
    ))
    dao.slice(Set(currency2Key)) mustBe LedgerState[ChimericStateValue](Map(
      currency2Key -> CreateCurrencyHolder(CreateCurrency(currency2))
    ))
    dao.slice(Set(missingKey)) mustBe LedgerState[ChimericStateValue](Map())
    dao.slice(Set(addressKey, utxorefKey, currency1Key, missingKey)) mustBe LedgerState[ChimericStateValue](Map(
      addressKey -> ValueHolder(addressValue),
      utxorefKey -> ValueHolder(utxorefValue),
      currency1Key -> CreateCurrencyHolder(CreateCurrency(currency1))
    ))
  }

}
