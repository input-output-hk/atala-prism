package io.iohk.cef.ledger

trait LedgerState[K, V] {
  def equals(that: LedgerState[K, V]): Boolean
  def get(key: K): Option[V]
  def contains(key: K): Boolean
}
