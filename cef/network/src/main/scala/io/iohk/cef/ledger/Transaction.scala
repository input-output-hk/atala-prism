package io.iohk.cef.ledger

trait Transaction[Key, Value] extends (LedgerState[Key, Value] => Either[LedgerError, LedgerState[Key, Value]]) {
  def keys: Set[Key]
}
