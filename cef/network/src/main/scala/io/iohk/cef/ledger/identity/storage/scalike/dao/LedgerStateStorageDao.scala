package io.iohk.cef.ledger.identity.storage.scalike.dao

import akka.util.ByteString
import io.iohk.cef.ledger.Partitioned
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
    Partitioned(aggregatedEntries.map)
  }

  def update(previousState: IdentityLedgerState,
             newState: IdentityLedgerState)(implicit session: DBSession): Unit = {
    val currentState = slice(previousState.keys)
    if (previousState != currentState) {
      throw new IllegalArgumentException("Provided previous state must be equal to the current state")
    } else {
      val keysToAdd = (newState.keys diff currentState.keys).toList
      val keysToRemove = (currentState.keys diff newState.keys).toList
      keysToAdd.map(key => newState.get(key).getOrElse(Set()).map(insert(key, _)))
      keysToRemove.map(key => currentState.get(key).getOrElse(Set()).map(remove(key, _)))
      (newState.keys intersect currentState.keys).map { key => {
        val values = newState.get(key).getOrElse(Set())
        val valuesToAdd = (values diff currentState.get(key).getOrElse(Set()))
        val valuesToRemove = (currentState.get(key).getOrElse(Set()) diff values)
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
