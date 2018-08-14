package io.iohk.cef.ledger.chimeric.storage.scalike

import io.iohk.cef.ledger.chimeric.storage.scalike.dao.ChimericLedgerStateStorageDao
import io.iohk.cef.ledger.chimeric.{ChimericLedgerState, ChimericStateValue}
import io.iohk.cef.ledger.storage.LedgerStateStorage
import scalikejdbc.{ConnectionPool, DB, DBSession}

class LedgerStateStorageImpl(ledgerStateStorageDao: ChimericLedgerStateStorageDao)
  extends LedgerStateStorage[ChimericStateValue] {

  override def slice(keys: Set[String]): ChimericLedgerState = {
    execInSession{ implicit session =>
      ledgerStateStorageDao.slice(keys)
    }
  }

  override def update(previousState: ChimericLedgerState,
                      newState: ChimericLedgerState): Unit = {
    execInSession{ implicit session =>
      ledgerStateStorageDao.update(previousState, newState)
    }
  }

  protected def execInSession[T](block: DBSession => T): T = DB(ConnectionPool.borrow()).localTx(block)

}
