package io.iohk.cef.ledger

import io.iohk.cef.ledger.storage.{Ledger, LedgerStateStorage, LedgerStorage}
import scalikejdbc._

trait LedgerFixture {

  def createLedger[S](stateStorage: LedgerStateStorage[S], storage: LedgerStorage)(
      implicit dBSession: DBSession): Ledger[S] = {

    Ledger(1, storage, stateStorage)
  }
}
