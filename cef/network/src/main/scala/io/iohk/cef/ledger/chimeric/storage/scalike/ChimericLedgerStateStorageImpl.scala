package io.iohk.cef.ledger.chimeric.storage.scalike

import io.iohk.cef.ledger.LedgerState
import io.iohk.cef.ledger.chimeric.ChimericStateValue
import io.iohk.cef.ledger.chimeric.storage.scalike.dao.ChimericLedgerStateStorageDao
import io.iohk.cef.ledger.storage.LedgerStateStorage
import scalikejdbc.{ConnectionPool, DB, DBSession}

class ChimericLedgerStateStorageImpl(ledgerStateStorageDao: ChimericLedgerStateStorageDao)
  extends LedgerStateStorage[ChimericStateValue] {

  override def slice(keys: Set[String]): LedgerState[ChimericStateValue] = {
    execInSession{ implicit session =>
      ledgerStateStorageDao.slice(keys)
    }
  }

  override def update(previousState: LedgerState[ChimericStateValue],
                      newState: LedgerState[ChimericStateValue]): Unit = {
    execInSession{ implicit session =>
      ledgerStateStorageDao.update(previousState, newState)
    }
  }

  protected def execInSession[T](block: DBSession => T): T = DB(ConnectionPool.borrow()).localTx(block)

}
