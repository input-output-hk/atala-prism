package io.iohk.cef.ledger.identity.storage.scalike.dao

import akka.util.ByteString
import io.iohk.cef.ledger.LedgerState
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
      updateActions.insert.map{case (key, set) => set.map(insert(key, _))}
      updateActions.delete.map{case (key, set) => set.map(remove(key, _))}
      updateActions.update.map { case (key, set) => {
        val valuesToAdd = (set diff currentState.get(key).getOrElse(Set()))
        val valuesToRemove = (currentState.get(key).getOrElse(Set()) diff set)
        for {
          _ <- valuesToAdd.map(v => insert(key, v))
          _ <- valuesToRemove.map(v => remove(key, v))
        } yield ()
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
