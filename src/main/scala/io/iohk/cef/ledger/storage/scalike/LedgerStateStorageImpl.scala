package io.iohk.cef.ledger.storage.scalike
import io.iohk.cef.ledger.storage.LedgerStateStorage
import io.iohk.cef.ledger.storage.scalike.dao.LedgerStateStorageDao
import io.iohk.cef.ledger.{ByteStringSerializable, LedgerState}
import scalikejdbc.{ConnectionPool, DB, DBSession, using}

case class LedgerStateStorageImpl[S](ledgerStateId: Int, ledgerStateDao: LedgerStateStorageDao[S])(
    implicit serializer: ByteStringSerializable[S])
    extends LedgerStateStorage[S] {

  override def slice(keys: Set[String]): LedgerState[S] =
    execInSession(implicit session => ledgerStateDao.slice(ledgerStateId, keys))

  override def update(previousState: LedgerState[S], newState: LedgerState[S]): Unit =
    execInSession(implicit session => ledgerStateDao.update(ledgerStateId, previousState, newState))

  protected def execInSession[T](block: DBSession => T): T =
    using(ConnectionPool.borrow()) { conn =>
      DB(conn).localTx(block)
    }
}
