package io.iohk.cef.ledger.storage

import io.iohk.cef.ledger.LedgerState

trait LedgerStateStorage[Key, Value] {

  def slice(keys: Set[Key]): LedgerState[Key, Value]

  def update(previousState: LedgerState[Key, Value], newState: LedgerState[Key, Value]): Unit
}
