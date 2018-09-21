package io.iohk.cef.ledger.chimeric.storage.scalike.dao

import io.iohk.cef.ledger.chimeric._
import io.iohk.cef.ledger.chimeric.storage.scalike._
import io.iohk.cef.ledger.storage.scalike.DataLayerException
import io.iohk.cef.ledger.{DeleteStateAction, InsertStateAction, LedgerState, UpdateStateAction}
import scalikejdbc._

class ChimericLedgerStateStorageDao {

  import ChimericLedgerState._

  def slice(keys: Set[String])(implicit DBSession: DBSession): ChimericLedgerState = {
    val stateKeys = keys.map(ChimericLedgerState.toStateKey)
    val currencies = readCurrencies(stateKeys.collect { case ch: CurrencyKey => ch })
      .map { createCurr =>
        getCurrencyPartitionId(createCurr.currency) -> CreateCurrencyHolder(createCurr)
      }

    val utxos = readUtxos(stateKeys.collect { case uh: UtxoValueKey => uh })
      .map { utxoPair =>
        getUtxoPartitionId(utxoPair._1) -> ValueHolder(utxoPair._2)
      }

    val addresses = readAddresses(stateKeys.collect { case ad: AddressValueKey => ad })
      .map {
        case (address, value) =>
          getAddressValuePartitionId(address) -> ValueHolder(value)
      }

    val nonces = readNonces(stateKeys.collect { case key: AddressNonceKey => key })
      .map {
        case (address, nonce) =>
          getAddressNoncePartitionId(address) -> NonceHolder(nonce)
      }

    val stateSequence = currencies ++ utxos ++ addresses ++ nonces

    LedgerState[ChimericStateValue](stateSequence.toMap)
  }

  def update(previousState: ChimericLedgerState, newState: ChimericLedgerState)(implicit DBsession: DBSession): Unit = {
    val currentState = slice(previousState.keys)
    if (previousState != currentState) {
      throw new IllegalArgumentException("Provided previous state must be equal to the current state")
    } else {
      val updateActions = currentState.updateTo(newState).mapKeys(ChimericLedgerState.toStateKey)
      updateActions.actions.foreach {
        case InsertStateAction(key: CurrencyKey, value: CreateCurrencyHolder) =>
          insertCurrency(key.currency -> value.createCurrency)
        case InsertStateAction(key: AddressValueKey, value: ValueHolder) =>
          insertAddress(key.address -> value.value)
        case InsertStateAction(key: AddressNonceKey, value: NonceHolder) =>
          insertNonce(key.address, value.nonce)
        case InsertStateAction(key: UtxoValueKey, value: ValueHolder) =>
          insertUtxo(key.txOutRef -> value.value)

        case DeleteStateAction(key: AddressValueKey, _) => deleteAddress(key.address)
        case DeleteStateAction(key: UtxoValueKey, _) => deleteUtxo(key.txOutRef)

        case UpdateStateAction(key: AddressValueKey, value: ValueHolder) =>
          deleteAddress(key.address)
          insertAddress((key.address, value.value))
        case UpdateStateAction(key: AddressNonceKey, value: NonceHolder) =>
          deleteNonce(key.address)
          insertNonce(key.address, value.nonce)

        case a => throw new IllegalArgumentException(s"Unexpected action: ${a}")
      }
    }
  }

  private def getStateEntryId(stringId: String)(implicit DBSession: DBSession): Option[Long] = {
    val se = ChimericLedgerStateEntryTable.syntax("se")
    sql"""
       select ${se.result.*}
       from ${ChimericLedgerStateEntryTable as se}
       where ${se.stringId} = ${stringId}
     """.map(ChimericLedgerStateEntryTable(se.resultName)(_).id).toOption().apply()
  }

  private def insertStateEntry(stringId: String)(implicit DBSession: DBSession): Long = {
    val column = ChimericLedgerStateEntryTable.column
    sql"""
       insert into ${ChimericLedgerStateEntryTable.table} (${column.stringId})
       values (${stringId})
       """.update().apply()
    val idOpt = getStateEntryId(stringId)
    idOpt.getOrElse(throw new DataLayerException(s"Could not insert a new Chimeric ledger entry: ${stringId}"))
  }

  private def deleteStateEntry(id: Long)(implicit DBSession: DBSession): Unit = {
    val column = ChimericLedgerStateEntryTable.column
    sql"""
       delete from ${ChimericLedgerStateEntryTable.table}
       where ${column.id} = ${id}
       """.update().apply()
  }

  private def readCurrencies(currencyKeys: Set[CurrencyKey])(implicit DBSession: DBSession): Seq[CreateCurrency] = {
    val cr = ChimericLedgerStateCurrencyTable.syntax("cr")
    val addresses =
      sql"""
         select ${cr.result.*}
         from ${ChimericLedgerStateCurrencyTable as cr}
         where ${cr.currency} in (${currencyKeys.map(_.currency)})
         """.map(ChimericLedgerStateCurrencyTable(cr.resultName)(_)).list().apply()
    addresses.map(_.toCreateCurrency)
  }

  private def insertCurrency(currency: (Currency, CreateCurrency))(implicit DBSession: DBSession): Unit = {
    val column = ChimericLedgerStateCurrencyTable.column
    val entryId = insertStateEntry(ChimericLedgerState.getCurrencyPartitionId(currency._1))
    sql"""
      insert into ${ChimericLedgerStateCurrencyTable.table} (${column.id}, ${column.currency})
      values (${entryId}, ${currency._2.currency})
      """.update().apply()
  }

  private def readUtxos(stateKeys: Set[UtxoValueKey])(implicit DBSession: DBSession): Seq[(TxOutRef, Value)] =
    stateKeys.toSeq.map(readUtxo(_).toSeq).flatten

  private def readUtxo(stateKeys: UtxoValueKey)(implicit DBSession: DBSession): Option[(TxOutRef, Value)] = {
    val ut = ChimericLedgerStateUtxoTable.syntax("ut")
    val utxo =
      sql"""
         select ${ut.result.*}
         from ${ChimericLedgerStateUtxoTable as ut}
         where ${ut.txId} = ${stateKeys.txOutRef.txId} and ${ut.index} = ${stateKeys.txOutRef.index}
         """.map(ChimericLedgerStateUtxoTable(ut.resultName)(_)).toOption().apply()
    utxo.map(t => (TxOutRef(t.txId, t.index) -> readValue(t.id)))
  }

  private def insertUtxo(utxo: (TxOutRef, Value))(implicit DBSession: DBSession): Unit = {
    val column = ChimericLedgerStateUtxoTable.column
    val entryId = insertStateEntry(ChimericLedgerState.getUtxoPartitionId(utxo._1))
    sql"""
       insert into ${ChimericLedgerStateUtxoTable.table} (${column.id}, ${column.txId}, ${column.index})
       values (${entryId}, ${utxo._1.txId}, ${utxo._1.index})
       """.update().apply()
    insertValue(entryId, utxo._2)
  }

  private def deleteUtxo(utxo: TxOutRef)(implicit DBSession: DBSession): Unit = {
    val column = ChimericLedgerStateUtxoTable.column
    val entryId =
      getStateEntryId(ChimericLedgerState.getUtxoPartitionId(utxo))
        .getOrElse(throw new DataLayerException(s"address not found: ${utxo}"))
    sql"""
       delete from ${ChimericLedgerStateUtxoTable.table}
       where ${column.txId} = ${utxo.txId} and ${column.index} = ${utxo.index}
       """.update().apply()
    deleteValue(entryId)
    deleteStateEntry(entryId)
  }

  private def readAddresses(stateKeys: Set[AddressValueKey])(implicit DBSession: DBSession): Seq[(Address, Value)] = {
    val ad = ChimericLedgerStateAddressTable.syntax("ad")
    val addresses =
      sql"""
         select ${ad.result.*}
         from ${ChimericLedgerStateAddressTable as ad}
         where ${ad.address} in (${stateKeys.map(_.address)})
         """.map(ChimericLedgerStateAddressTable(ad.resultName)(_)).list().apply()
    addresses.map(t => (t.address, readValue(t.id)))
  }

  private def insertAddress(address: (Address, Value))(implicit DBSession: DBSession): Unit = {
    val column = ChimericLedgerStateAddressTable.column
    val entryId = insertStateEntry(ChimericLedgerState.getAddressValuePartitionId(address._1))
    sql"""
       insert into ${ChimericLedgerStateAddressTable.table} (${column.id}, ${column.address})
       values (${entryId}, ${address._1})
       """.update().apply()
    insertValue(entryId, address._2)
  }

  private def deleteAddress(address: Address)(implicit DBSession: DBSession): Unit = {
    val column = ChimericLedgerStateAddressTable.column
    val entryId =
      getStateEntryId(ChimericLedgerState.getAddressValuePartitionId(address))
        .getOrElse(throw new DataLayerException(s"address not found: ${address}"))
    sql"""
       delete from ${ChimericLedgerStateAddressTable.table}
       where ${column.address} = ${address}
       """.update().apply()
    deleteValue(entryId)
    deleteStateEntry(entryId)

    // NOTE: Deleting the nonce here is problematic, updates are a delete and then insert, the nonce will be lost.
  }

  private def readValue(ledgerStateEntryId: Long)(implicit DBSession: DBSession): Value = {
    val v = ChimericValueEntryTable.syntax("v")
    val entryList = sql"""
         select ${v.result.*}
         from ${ChimericValueEntryTable as v}
         where ${v.ledgerStateEntryId} = ${ledgerStateEntryId}
       """.map(rs => ChimericValueEntryTable(v.resultName)(rs)).list().apply()
    ChimericValueEntryTable.toValue(entryList)
  }

  private def insertValue(entryId: Long, value: Value)(implicit DBSession: DBSession) = {
    val column = ChimericValueEntryTable.column
    value.iterator.foreach {
      case (currency, quantity) =>
        sql"""
          insert into ${ChimericValueEntryTable.table}
          (${column.ledgerStateEntryId}, ${column.currency}, ${column.amount})
          values (${entryId}, ${currency}, ${quantity.toString})
        """.update().apply()
    }
  }

  private def deleteValue(entryId: Long)(implicit DBSession: DBSession): Unit = {
    val column = ChimericValueEntryTable.column
    sql"""
      delete from ${ChimericValueEntryTable.table}
      where ${column.ledgerStateEntryId} = ${entryId}
    """.update().apply()
  }

  private def readNonces(keys: Set[AddressNonceKey])(implicit DBSession: DBSession): Set[(Address, Int)] = {
    val nonceTable = ChimericLedgerStateNonceTable.syntax("nonce_table")
    val entryTable = ChimericLedgerStateEntryTable.syntax("entry_table")

    // TODO: optimize this
    keys.flatMap { key =>
      sql"""
         SELECT ${nonceTable.result.*}
         FROM ${ChimericLedgerStateNonceTable as nonceTable} JOIN
              ${ChimericLedgerStateEntryTable as entryTable}
              ON ${nonceTable.id} = ${entryTable.id}
         WHERE ${entryTable.stringId} = ${getAddressNoncePartitionId(key.address)}
         """
        .map(ChimericLedgerStateNonceTable(nonceTable.resultName)(_))
        .headOption()
        .apply()
        .map(key.address -> _.nonce)
    }
  }

  private def deleteNonce(address: Address)(implicit DBSession: DBSession): Unit = {
    val column = ChimericLedgerStateNonceTable.column
    val entryId = getStateEntryId(getAddressNoncePartitionId(address))
      .getOrElse(throw DataLayerException(s"address not found: $address"))

    val _ = sql"""
       DELETE FROM ${ChimericLedgerStateNonceTable.table}
       WHERE ${column.id} = $entryId
       """
      .update()
      .apply()

    deleteStateEntry(entryId)
  }

  private def insertNonce(address: Address, nonce: Int)(implicit DBSession: DBSession): Unit = {
    val column = ChimericLedgerStateNonceTable.column
    val entryId = insertStateEntry(getAddressNoncePartitionId(address))

    val _ = sql"""
       INSERT INTO ${ChimericLedgerStateNonceTable.table}
         (${column.id}, ${column.nonce})
       VALUES
         ($entryId, $nonce)
       """
      .update()
      .apply()
  }
}
