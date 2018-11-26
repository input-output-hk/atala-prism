package io.iohk.cef.ledger

import io.iohk.cef.ledger.storage.{Ledger, LedgerStateStorage, LedgerStorage}

trait LedgerFixture {
  def createLedger(stateStorage: LedgerStateStorage, storage: LedgerStorage): Ledger =
    Ledger("1", storage, stateStorage)
}
