package io.iohk.cef.ledger.identity

import io.iohk.cef.crypto._
import io.iohk.cef.ledger.identity.storage.scalike.IdentityLedgerStateTable
import scalikejdbc._

trait IdentityLedgerStateStorageFixture {

  def insertPairs(pairs: List[(String, SigningPublicKey)])(implicit session: DBSession): Unit = {
    val column = IdentityLedgerStateTable.column
    pairs.foreach(item => {
      val (identity, publicKey) = item
      sql"""
            insert into ${IdentityLedgerStateTable.table} (${column.identity}, ${column.publicKey})
              values ($identity, ${publicKey.toByteString.toArray})
            """.executeUpdate.apply()
    })
  }
}
