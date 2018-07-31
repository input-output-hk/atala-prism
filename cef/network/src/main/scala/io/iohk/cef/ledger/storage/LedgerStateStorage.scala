package io.iohk.cef.ledger.storage

import io.iohk.cef.ledger.LedgerState

trait LedgerStateStorage[S] {

  def slice(keys: Set[String]): LedgerState[S]

  def update(previousState: LedgerState[S], newState: LedgerState[S]): Unit
}
