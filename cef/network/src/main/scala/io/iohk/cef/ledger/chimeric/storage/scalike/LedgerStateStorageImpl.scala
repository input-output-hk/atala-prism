package io.iohk.cef.ledger.chimeric.storage.scalike

import io.iohk.cef.ledger.LedgerState
import io.iohk.cef.ledger.chimeric.ChimericStateValue
import io.iohk.cef.ledger.chimeric.storage.scalike.dao.LedgerStateStorageDao
import io.iohk.cef.ledger.storage.LedgerStateStorage
import scalikejdbc.{ConnectionPool, DB, DBSession}

class LedgerStateStorageImpl(ledgerStateStorageDao: LedgerStateStorageDao)
  extends LedgerStateStorage[ChimericStateValue] {

  override def slice(keys: Set[String]): LedgerState[ChimericStateValue] = {
    execInSession{ implicit session => ???
    }
  }

  override def update(previousState: LedgerState[ChimericStateValue],
                      newState: LedgerState[ChimericStateValue]): Unit = ???

  protected def execInSession[T](block: DBSession => T): T = DB(ConnectionPool.borrow()).localTx(block)

}
