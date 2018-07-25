package io.iohk.cef.ledger.identity

import akka.util.ByteString
import io.iohk.cef.ledger.identity.storage.scalike.IdentityLedgerStateTable
import scalikejdbc._

trait LedgerStateStorageFixture {

  def insertPairs(pairs: List[(String, ByteString)])(implicit session: DBSession): Unit = {
    val column = IdentityLedgerStateTable.column
    pairs.foreach(item => {
      val (identity, publicKey) = item
      sql"""
            insert into ${IdentityLedgerStateTable.table} (${column.identity}, ${column.publicKey})
              values (${identity}, ${publicKey.toArray})
            """.executeUpdate.apply()
    })
  }
}
