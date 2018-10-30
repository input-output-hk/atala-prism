package io.iohk.cef.ledger.chimeric.storage.scalike.dao

import io.iohk.cef.crypto
import io.iohk.cef.crypto.SigningPublicKey
import io.iohk.cef.ledger.LedgerState
import io.iohk.cef.ledger.chimeric._
import io.iohk.cef.ledger.chimeric.storage.scalike._
import org.scalatest.{MustMatchers, fixture}
import scalikejdbc._
import scalikejdbc.scalatest.AutoRollback

trait ChimericLedgerStateStorageDaoDbTest extends fixture.FlatSpec with AutoRollback with MustMatchers {

  def insertPairs(pairs: Seq[(String, ChimericStateResult)])(implicit DBSession: DBSession): Unit = {
    val column = ChimericLedgerStateEntryTable.column
    val se = ChimericLedgerStateEntryTable.syntax("se")
    pairs.foreach(item => {
      val (key, value) = item
      sql"""
            insert into ${ChimericLedgerStateEntryTable.table} (${column.stringId})
              values ($key)
            """.update.apply()
      val entryId =
        sql"""
           select ${se.result.*}
           from ${ChimericLedgerStateEntryTable as se}
           where ${se.stringId} = $key
           """.map(ChimericLedgerStateEntryTable(se.resultName)(_)).toOption().apply().get.id
      insertKey(entryId, key)
      insertValue(entryId, value)
    })
  }

  private def insertKey(entryId: Long, key: String)(implicit DBSession: DBSession): Unit = {
    ChimericLedgerState.toStateKey(key) match {
      case AddressQuery(address) =>
        val column = ChimericLedgerStateAddressTable.column
        sql"""
             insert into ${ChimericLedgerStateAddressTable.table}
              (${column.id}, ${column.address})
              values (${entryId}, ${address})
            """.update.apply()
      case AddressNonceQuery(address) => ???
      case UtxoQuery(txOutRef) =>
        val column = ChimericLedgerStateUtxoTable.column
        sql"""
             insert into ${ChimericLedgerStateUtxoTable.table}
              (${column.id}, ${column.txId}, ${column.index})
              values (${entryId}, ${txOutRef.txId}, ${txOutRef.index})
            """.update.apply()
      case CurrencyQuery(currency) =>
        val column = ChimericLedgerStateCurrencyTable.column
        sql"""
             insert into ${ChimericLedgerStateCurrencyTable.table}
              (${column.id}, ${column.currency})
              values (${entryId}, ${currency})
            """.update.apply()
    }
  }

  private def insertValueSQL(entryId: Long, value: Value)(implicit DBSession: DBSession) = {
    val column = ChimericValueEntryTable.column
    value.iterator.foreach {
      case (currency, quantity) =>
        sql"""
             insert into ${ChimericValueEntryTable.table}
              (${column.ledgerStateEntryId}, ${column.currency}, ${column.amount})
              values (${entryId}, ${currency}, ${quantity})
            """.update.apply()
    }
  }
  private def insertValue(entryId: Long, value: ChimericStateResult)(implicit DBSession: DBSession): Unit = {
    value match {
      case NonceResult(_) => ()
      case CreateCurrencyResult(_) => ()
      case UtxoResult(v, signingPublicKey) =>
        insertValueSQL(entryId, v)
        updateUtxoSigningPublicKey(entryId, signingPublicKey)
      case AddressResult(v, signingPublicKey) =>
        insertValueSQL(entryId, v)
        updateAddressSigningPublicKey(entryId, signingPublicKey)
    }
  }

  private def updateUtxoSigningPublicKey(entryId: Long, signingPublicKey: Option[SigningPublicKey])(
      implicit DBSession: DBSession): Unit = {
    signingPublicKey.map { key =>
      val column = ChimericLedgerStateUtxoTable.column
      val keyBytes = key.toByteString.toArray
      sql"""
        update ${ChimericLedgerStateUtxoTable.table}
        set ${column.signingPublicKey} = ${keyBytes}
        where ${column.id} = ${entryId}
      """.update().apply()
    }
  }

  private def updateAddressSigningPublicKey(entryId: Long, signingPublicKey: Option[SigningPublicKey])(
      implicit DBSession: DBSession): Unit = {
    signingPublicKey.map { key =>
      val column = ChimericLedgerStateAddressTable.column
      val keyBytes = key.toByteString.toArray
      sql"""
        update ${ChimericLedgerStateAddressTable.table}
        set ${column.signingPublicKey} = ${keyBytes}
        where ${column.id} = ${entryId}
      """.update().apply()
    }
  }

  behavior of "LedgerStateStorageDao"

  it should "retrieve a slice" in { implicit s =>
    val address = "address"
    val currency1 = "currency"
    val currency2 = "currency2"
    val addressValue = Value(Map(currency1 -> BigDecimal(1.5), currency2 -> BigDecimal(0.5)))
    val utxoref = TxOutRef("txId", 1)
    val utxorefValue = Value(
      Map(
        currency1 -> BigDecimal(2.99999),
        currency2 -> BigDecimal(3.5)
      ))
    val signingPublicKey = Some(crypto.generateSigningKeyPair().public)
    val utxoResult = UtxoResult(utxorefValue, signingPublicKey)
    val addressKey = ChimericLedgerState.getAddressPartitionId(address)
    val currency1Key = ChimericLedgerState.getCurrencyPartitionId(currency1)
    val currency2Key = ChimericLedgerState.getCurrencyPartitionId(currency2)
    val utxorefKey = ChimericLedgerState.getUtxoPartitionId(utxoref)
    val missingKey = ChimericLedgerState.getAddressPartitionId("")
    val pairs = Seq(
      addressKey -> AddressResult(addressValue, signingPublicKey),
      utxorefKey -> utxoResult,
      currency1Key -> CreateCurrencyResult(CreateCurrency(currency1)),
      currency2Key -> CreateCurrencyResult(CreateCurrency(currency2))
    )
    insertPairs(pairs)
    val dao = new ChimericLedgerStateStorageDao()
    dao.slice(Set(addressKey)) mustBe LedgerState[ChimericStateResult](
      Map(
        addressKey -> AddressResult(addressValue, signingPublicKey)
      ))
    dao.slice(Set(utxorefKey)) mustBe LedgerState[ChimericStateResult](
      Map(
        utxorefKey -> utxoResult
      ))
    dao.slice(Set(currency1Key)) mustBe LedgerState[ChimericStateResult](
      Map(
        currency1Key -> CreateCurrencyResult(CreateCurrency(currency1))
      ))
    dao.slice(Set(currency2Key)) mustBe LedgerState[ChimericStateResult](
      Map(
        currency2Key -> CreateCurrencyResult(CreateCurrency(currency2))
      ))
    dao.slice(Set(missingKey)) mustBe LedgerState[ChimericStateResult](Map())
    dao.slice(Set(addressKey, utxorefKey, currency1Key, missingKey)) mustBe LedgerState[ChimericStateResult](
      Map(
        addressKey -> AddressResult(addressValue, signingPublicKey),
        utxorefKey -> utxoResult,
        currency1Key -> CreateCurrencyResult(CreateCurrency(currency1))
      ))
  }

}
