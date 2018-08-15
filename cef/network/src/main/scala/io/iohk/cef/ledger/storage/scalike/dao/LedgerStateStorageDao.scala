package io.iohk.cef.ledger.storage.scalike.dao

import akka.util.ByteString
import io.iohk.cef.ledger.storage.scalike.LedgerStateTable
import io.iohk.cef.ledger._
import scalikejdbc._

class LedgerStateStorageDao[S] {

  def slice(ledgerStateId: Int, keys: Set[String])(
    implicit byteStringSerializable: ByteStringSerializable[S],
    DBSession: DBSession): LedgerState[S] = {
    val lst = LedgerStateTable.syntax("lst")
    val entries = sql"""
       select ${lst.result.*}
       from ${LedgerStateTable as lst}
       where ${lst.resultName.ledgerStateId} = ${ledgerStateId} and
        ${lst.resultName.partitionId} in (${keys})
       """.map(rs => LedgerStateTable(lst.resultName)(rs)).list().apply()
    val pairs = entries.map(entry => (entry.partitionId -> byteStringSerializable.deserialize(entry.data)))
    LedgerState(Map(pairs:_*))
  }

  def update(ledgerStateId: Int, previousState: LedgerState[S], newState: LedgerState[S])(
    implicit byteStringSerializable: ByteStringSerializable[S],
    DBSession: DBSession): Unit = {
    val currentState = slice(ledgerStateId, previousState.keys)
    if (previousState != currentState) {
      throw new IllegalArgumentException("Provided previous state must be equal to the current state")
    } else {
      val actions = previousState.updateTo(newState)
      actions.actions.foreach(_ match {
        case InsertStateAction(key, value) => insertEntry(ledgerStateId, key, byteStringSerializable.serialize(value))
        case DeleteStateAction(key, value) => deleteEntry(ledgerStateId, key, byteStringSerializable.serialize(value))
        case UpdateStateAction(key, value) => updateEntry(ledgerStateId, key, byteStringSerializable.serialize(value))
      })
    }
  }

  private def insertEntry(ledgerStateId: Int, key: String, value: ByteString)(implicit DBSession: DBSession) = {
    val column = LedgerStateTable.column
    sql"""
       insert into ${LedgerStateTable.table}
        (${column.ledgerStateId}, ${column.partitionId}, ${column.data})
        values (${ledgerStateId}, ${key}, ${value})
       """.update.apply
  }

  private def deleteEntry(ledgerStateId: Int, key: String, value: ByteString)(implicit DBSession: DBSession) = {
    val column = LedgerStateTable.column
    sql"""
       delete from ${LedgerStateTable.table}
       where ${column.ledgerStateId} = ${ledgerStateId} and ${column.partitionId} = ${key}
       """.update.apply()
  }

  private def updateEntry(ledgerStateId: Int, key: String, value: ByteString)(implicit DBSession: DBSession) = {
    val column = LedgerStateTable.column
    sql"""
       update ${LedgerStateTable.table} set ${column.data} = ${value}
       where ${column.ledgerStateId} = ${ledgerStateId} and ${column.partitionId} = ${key}
       """.update.apply()
  }
}
