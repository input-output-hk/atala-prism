package io.iohk.cef.ledger.identity.storage.scalike.dao

import akka.util.ByteString
import io.iohk.cef.ledger.{DeleteStateUpdate, InsertStateUpdate, LedgerState, UpdateStateUpdate}
import io.iohk.cef.ledger.identity.IdentityLedgerState
import io.iohk.cef.ledger.identity.storage.scalike.{IdentityLedgerStateTable, LedgerStateEntryMap}
import org.bouncycastle.util.encoders.Hex
import scalikejdbc._

class LedgerStateStorageDao {

  def slice(keys: Set[String])(implicit session: DBSession): IdentityLedgerState = {
    val st = IdentityLedgerStateTable.syntax("st")
    val pairs =
      sql"""
      select ${st.result.*} from ${IdentityLedgerStateTable as st}
       where ${st.identity} in (${keys})
      """.map(rs => IdentityLedgerStateTable(st.resultName)(rs)).list.apply()
    val emptyEntries = LedgerStateEntryMap[String, ByteString]()
    val aggregatedEntries =
      pairs.foldLeft(emptyEntries)(_ aggregateWith _)
    LedgerState(aggregatedEntries.map)
  }

  def update(previousState: IdentityLedgerState,
             newState: IdentityLedgerState)(implicit session: DBSession): Unit = {
    val currentState = slice(previousState.keys)
    if (previousState != currentState) {
      throw new IllegalArgumentException("Provided previous state must be equal to the current state")
    } else {
      val updateActions = currentState.updateTo(newState)
      updateActions.actions.foreach {
        case InsertStateUpdate(key, set) => set.foreach(insert(key, _))
        case DeleteStateUpdate(key, set) => set.foreach(remove(key, _))
        case UpdateStateUpdate(key, set) => {
          val valuesToAdd = (set diff currentState.get(key).getOrElse(Set()))
          val valuesToRemove = (currentState.get(key).getOrElse(Set()) diff set)
          valuesToAdd.foreach(v => insert(key, v))
          valuesToRemove.foreach(v => remove(key, v))
        }
      }
    }
  }

  def insert(identity: String, publicKey: ByteString)(implicit session: DBSession): Int = {
    val column = IdentityLedgerStateTable.column
    sql"""
      insert into ${IdentityLedgerStateTable.table} (${column.identity}, ${column.publicKey})
        values (${identity}, ${publicKey.toArray})
      """.executeUpdate.apply()
  }

  def remove(identity: String, publicKey: ByteString)(implicit session: DBSession): Int = {
    val column = IdentityLedgerStateTable.column
    sql"""
      delete from ${IdentityLedgerStateTable.table}
       where ${column.identity} = ${identity} and ${column.publicKey} = ${Hex.toHexString(publicKey.toArray)}
      """.executeUpdate.apply()
  }
}
