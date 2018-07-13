package io.iohk.cef.ledger.identity

import akka.util.ByteString
import io.iohk.cef.ledger.identity.storage.LedgerStateStorageImpl
import io.iohk.cef.ledger.identity.storage.db.IdentityLedgerStateTable
import scalikejdbc._

import scala.concurrent.Future

trait LedgerStateStorageFixture {


  def createStorage(session: DBSession) = new LedgerStateStorageImpl {
    override def createDb: DB = null

    override def inTx[T](db: DB)(block: DBSession => T): T = block(session)

    override def readOnly[T](db: DB)(block: DBSession => T): T = block(session)

    override def begin[T](f: DB => Future[T]): Future[T] = f(createDb)
  }

  def insertPairs(pairs: List[(String, ByteString)])(implicit session: DBSession) = {
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
