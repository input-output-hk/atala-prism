package io.iohk.cef.ledger.identity.storage.scalike

import akka.util.ByteString
import io.iohk.cef.ledger.identity.IdentityLedgerState
import io.iohk.cef.ledger.identity.storage.scalike.dao.IdentityLedgerStateStorageDao
import io.iohk.cef.ledger.storage.LedgerStateStorage
import scalikejdbc._

class IdentityLedgerStateStorageImpl(ledgerStateStorageDao: IdentityLedgerStateStorageDao)
  extends LedgerStateStorage[Set[ByteString]] {

  override def slice(keys: Set[String]): IdentityLedgerState = {
    execInSession { implicit session =>
      ledgerStateStorageDao.slice(keys)
    }
  }

  override def update(previousState: IdentityLedgerState, newState: IdentityLedgerState): Unit = {
    execInSession { implicit session =>
      ledgerStateStorageDao.update(previousState, newState)
    }
  }

  protected def execInSession[T](block: DBSession => T): T =
    using(ConnectionPool.borrow()) { conn =>
      DB(conn).localTx(block)
    }
}
