package io.iohk.cef.ledger.storage

import io.iohk.cef.ledger.LedgerState

trait LedgerStateStorage[State <: LedgerState[Key, _], Key] {

  def slice(keys: Set[Key]): State

  def update(previousState: State, newState: State): Unit
}
