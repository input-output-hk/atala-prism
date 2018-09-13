package io.iohk.cef.ledger.storage.scalike.dao

import akka.util.ByteString
import io.iohk.cef.ledger._
import io.iohk.cef.ledger.storage.scalike.LedgerStateTable
import scalikejdbc._

class LedgerStateStorageDao[S] {

  def slice(ledgerStateId: Int, keys: Set[String])(
      implicit byteStringSerializable: ByteStringSerializable[S],
      DBSession: DBSession): LedgerState[S] = {
    val lst = LedgerStateTable.syntax("lst")
    val entries = sql"""
       select ${lst.result.*}
       from ${LedgerStateTable as lst}
       where ${lst.ledgerStateId} = ${ledgerStateId} and
        ${lst.partitionId} in (${keys})
       """.map(rs => LedgerStateTable(lst.resultName)(rs)).list().apply()
    val pairs = entries.map(entry => (entry.partitionId -> byteStringSerializable.decode(entry.data)))
    val flattenedPairs = pairs.collect { case (identity, Some(key)) => (identity, key) }
    if (flattenedPairs.size != pairs.size)
      throw new IllegalArgumentException(s"Could not parse all entries: ${pairs.filter(_._2.isEmpty).map(_._1)}")
    LedgerState(Map(flattenedPairs: _*))
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
        case InsertStateAction(key, value) => insertEntry(ledgerStateId, key, byteStringSerializable.encode(value))
        case DeleteStateAction(key, _) => deleteEntry(ledgerStateId, key)
        case UpdateStateAction(key, value) => updateEntry(ledgerStateId, key, byteStringSerializable.encode(value))
      })
    }
  }

  private def insertEntry(ledgerStateId: Int, key: String, value: ByteString)(implicit DBSession: DBSession) = {
    val column = LedgerStateTable.column
    sql"""
       insert into ${LedgerStateTable.table}
        (${column.ledgerStateId}, ${column.partitionId}, ${column.data})
        values (${ledgerStateId}, ${key}, ${value.toArray})
       """.update.apply
  }

  private def deleteEntry(ledgerStateId: Int, key: String)(implicit DBSession: DBSession) = {
    val column = LedgerStateTable.column
    sql"""
       delete from ${LedgerStateTable.table}
       where ${column.ledgerStateId} = ${ledgerStateId} and ${column.partitionId} = ${key}
       """.update.apply()
  }

  private def updateEntry(ledgerStateId: Int, key: String, value: ByteString)(implicit DBSession: DBSession) = {
    val column = LedgerStateTable.column
    sql"""
       update ${LedgerStateTable.table} set ${column.data} = ${value.toArray}
       where ${column.ledgerStateId} = ${ledgerStateId} and ${column.partitionId} = ${key}
       """.update.apply()
  }
}
