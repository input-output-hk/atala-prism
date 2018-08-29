package io.iohk.cef.ledger.identity

import java.security.PublicKey

import io.iohk.cef.ledger.identity.storage.scalike.IdentityLedgerStateTable
import scalikejdbc._

trait IdentityLedgerStateStorageFixture {

  def insertPairs(pairs: List[(String, PublicKey)])(implicit session: DBSession): Unit = {
    val column = IdentityLedgerStateTable.column
    pairs.foreach(item => {
      val (identity, publicKey) = item
      sql"""
            insert into ${IdentityLedgerStateTable.table} (${column.identity}, ${column.publicKey})
              values (${identity}, ${publicKey.getEncoded})
            """.executeUpdate.apply()
    })
  }
}
