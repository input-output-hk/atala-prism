package io.iohk.cef.ledger

import io.iohk.cef.ledger.storage.{Ledger, LedgerStateStorage, LedgerStorage}
import scalikejdbc._

trait LedgerFixture {

  def createLedger(stateStorage: LedgerStateStorage, storage: LedgerStorage)(implicit dBSession: DBSession): Ledger = {

    Ledger("1", storage, stateStorage)
  }
}
