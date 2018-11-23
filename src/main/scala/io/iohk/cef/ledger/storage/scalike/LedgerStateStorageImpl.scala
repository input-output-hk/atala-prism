package io.iohk.cef.ledger.storage.scalike
import io.iohk.cef.LedgerId
import io.iohk.cef.ledger.storage.LedgerStateStorage
import io.iohk.cef.ledger.storage.scalike.dao.LedgerStateStorageDao
import io.iohk.cef.ledger.LedgerState
import scalikejdbc.{ConnectionPool, DB, DBSession, using}
import io.iohk.cef.codecs.nio._
import scala.reflect.runtime.universe.TypeTag

class LedgerStateStorageImpl(ledgerStateId: LedgerId, ledgerStateDao: LedgerStateStorageDao)
    extends LedgerStateStorage {

  override def slice[S: NioEncDec: TypeTag](keys: Set[String]): LedgerState[S] =
    execInSession(implicit session => ledgerStateDao.slice(ledgerStateId, keys))

  override def update[S: NioEncDec: TypeTag](previousState: LedgerState[S], newState: LedgerState[S]): Unit =
    execInSession(implicit session => ledgerStateDao.update(ledgerStateId, previousState, newState))

  protected def execInSession[T](block: DBSession => T): T =
    using(ConnectionPool.borrow()) { conn =>
      DB(conn).localTx(block)
    }
}
