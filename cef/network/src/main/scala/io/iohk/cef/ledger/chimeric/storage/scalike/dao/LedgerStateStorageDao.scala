package io.iohk.cef.ledger.chimeric.storage.scalike.dao

import io.iohk.cef.ledger.LedgerState
import io.iohk.cef.ledger.chimeric.storage.scalike._
import io.iohk.cef.ledger.chimeric._
import scalikejdbc._

class LedgerStateStorageDao {

  def slice(keys: Set[String])(implicit DBSession: DBSession): LedgerState[ChimericStateValue] = {
    val stateKeys = keys.map(ChimericLedgerState.toStateKey)
    val currencies = readCurrencies(stateKeys.collect{ case ch: CurrencyHolder => ch })
    val utxos = readUtxos(stateKeys.collect{ case uh: UtxoHolder => uh })
    val addresses = readAddresses(stateKeys.collect{ case ad: AddressHolder => ad })
    val stateSequence: Seq[(String, ChimericStateValue)] =
      currencies.map(createCurr =>
        ChimericLedgerState.getCurrencyPartitionId(createCurr.currency) -> CreateCurrencyHolder(createCurr)
      ) ++
      utxos.map(utxoPair =>
        ChimericLedgerState.getPartitionId(utxoPair._1) -> ValueHolder(utxoPair._2)
      ) ++
      addresses.map(addressPair =>
        ChimericLedgerState.getAddressPartitionId(addressPair._1) -> ValueHolder(addressPair._2)
      )
    LedgerState[ChimericStateValue](stateSequence.toMap)
  }

  def update(previousState: LedgerState[ChimericStateValue],
             newState: LedgerState[ChimericStateValue]): Unit = {
    ???
  }

  private def readCurrencies(currencyKeys: Set[CurrencyHolder])(implicit DBSession: DBSession): Seq[CreateCurrency] = {
    val cr = ChimericLedgerStateCurrencyTable.syntax("cr")
    val addresses =
      sql"""
         select ${cr.result.*}
         from ${ChimericLedgerStateCurrencyTable as cr}
         where ${cr.resultName.currency} in ${currencyKeys.map(_.currency)}
         """.map(ChimericLedgerStateCurrencyTable(cr.resultName)(_)).list().apply()
    addresses.map(_.toCreateCurrency)
  }

  private def readUtxos(stateKeys: Set[UtxoHolder])(implicit DBSession: DBSession): Seq[(TxOutRef, Value)] =
    stateKeys.toSeq.map(readUtxo(_).toSeq).flatten

  private def readUtxo(stateKeys: UtxoHolder)(implicit DBSession: DBSession): Option[(TxOutRef, Value)] = {
    val ut = ChimericLedgerStateUtxoTable.syntax("ut")
    val utxo =
      sql"""
         select ${ut.result.*}
         from ${ChimericLedgerStateUtxoTable as ut}
         where ${ut.txId} = ${stateKeys.txOutRef.id} and ${ut.index} = ${stateKeys.txOutRef.index}
         """.map(ChimericLedgerStateUtxoTable(ut.resultName)(_)).toOption().apply()
    utxo.map(t => (TxOutRef(t.txId, t.index) -> readValue(t.id)))
  }

  private def readAddresses(stateKeys: Set[AddressHolder])(implicit DBSession: DBSession): Seq[(Address, Value)] = {
    val ad = ChimericLedgerStateAddressTable.syntax("ad")
    val addresses =
      sql"""
         select ${ad.result.*}
         from ${ChimericLedgerStateAddressTable as ad}
         where ${ad.resultName.address} in ${stateKeys.map(_.address)}
         """.map(ChimericLedgerStateAddressTable(ad.resultName)(_)).list().apply()
    addresses.map(t => (t.address, readValue(t.id)))
  }

  private def readValue(ledgerStateEntryId: Long)(implicit DBSession: DBSession): Value = {
    val v = ChimericValueEntryTable.syntax("v")
    val entryList = sql"""
         select ${v.result.*}
         from ${ChimericValueEntryTable as v}
         where ${v.resultName.ledgerStateEntryId} = ${ledgerStateEntryId}
       """.map(rs => ChimericValueEntryTable(v.resultName)(rs)).list().apply()
    ChimericValueEntryTable.toValue(entryList)
  }
}
