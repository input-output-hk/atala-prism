package io.iohk.cef.ledger.identity.storage.scalike

import io.iohk.cef.ledger.identity.IdentityLedgerState
import io.iohk.cef.ledger.identity.storage.scalike.dao.LedgerStateStorageDao
import io.iohk.cef.ledger.storage.LedgerStateStorage
import scalikejdbc._

import scala.concurrent.ExecutionContext

class LedgerStateStorageImpl(ledgerStateStorageDao: LedgerStateStorageDao)(implicit ec: ExecutionContext)
  extends LedgerStateStorage[IdentityLedgerState, String] {

  override def slice(keys: Set[String]): IdentityLedgerState = {
    val db = createDb
    db readOnly { implicit session =>
      ledgerStateStorageDao.slice(keys)
    }
  }

  override def update(previousState: IdentityLedgerState, newState: IdentityLedgerState): Unit = {
    val db = createDb
    db localTx { implicit session =>
      ledgerStateStorageDao.update(previousState, newState)
    }
  }

  protected def createDb: DB = {
    val conn = ConnectionPool.borrow()
    DB(conn)
  }
}
