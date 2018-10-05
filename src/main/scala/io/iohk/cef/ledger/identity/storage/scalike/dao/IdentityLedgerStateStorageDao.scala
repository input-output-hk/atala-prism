package io.iohk.cef.ledger.identity.storage.scalike.dao

import io.iohk.cef.crypto._
import io.iohk.cef.ledger.identity.IdentityLedgerState
import io.iohk.cef.ledger.identity.storage.scalike.{IdentityLedgerStateTable, LedgerStateEntryMap}
import io.iohk.cef.ledger.{DeleteStateAction, InsertStateAction, LedgerState, UpdateStateAction}
import io.iohk.cef.utils.HexStringCodec._
import scalikejdbc._

class IdentityLedgerStateStorageDao {

  def slice(keys: Set[String])(implicit session: DBSession): IdentityLedgerState = {
    val st = IdentityLedgerStateTable.syntax("st")
    val pairs =
      sql"""
      select ${st.result.*} from ${IdentityLedgerStateTable as st}
       where ${st.identity} in (${keys})
      """.map(rs => IdentityLedgerStateTable(st.resultName)(rs)).list.apply()
    val emptyEntries = LedgerStateEntryMap[String, SigningPublicKey]()
    val aggregatedEntries =
      pairs.foldLeft(emptyEntries)(_ aggregateWith _)
    LedgerState(aggregatedEntries.map)
  }

  def update(previousState: IdentityLedgerState, newState: IdentityLedgerState)(implicit session: DBSession): Unit = {
    val currentState = slice(previousState.keys)
    if (previousState != currentState) {
      throw new IllegalArgumentException("Provided previous state must be equal to the current state")
    } else {
      val updateActions = currentState.updateTo(newState)
      updateActions.actions.foreach {
        case InsertStateAction(key, set) => set.foreach(insert(key, _))
        case DeleteStateAction(key, set) => set.foreach(remove(key, _))
        case UpdateStateAction(key, set) => {
          val valuesToAdd = (set diff currentState.get(key).getOrElse(Set()))
          val valuesToRemove = (currentState.get(key).getOrElse(Set()) diff set)
          valuesToAdd.foreach(v => insert(key, v))
          valuesToRemove.foreach(v => remove(key, v))
        }
      }
    }
  }

  def insert(identity: String, publicKey: SigningPublicKey)(implicit session: DBSession): Int = {
    val column = IdentityLedgerStateTable.column
    sql"""
      insert into ${IdentityLedgerStateTable.table} (${column.identity}, ${column.publicKey})
        values (${identity}, ${publicKey.toByteString.toArray})
      """.executeUpdate.apply()
  }

  def remove(identity: String, publicKey: SigningPublicKey)(implicit session: DBSession): Int = {
    val column = IdentityLedgerStateTable.column
    sql"""
      delete from ${IdentityLedgerStateTable.table}
       where ${column.identity} = ${identity} and ${column.publicKey} = ${toHexString(publicKey.toByteString)}
      """.executeUpdate.apply()
  }
}
