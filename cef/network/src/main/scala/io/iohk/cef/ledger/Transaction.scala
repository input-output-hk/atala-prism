package io.iohk.cef.ledger

trait Transaction[State <: LedgerState[Key, _], Key] extends (State => Either[LedgerError, State]) {
  def keys: Set[Key]
}
