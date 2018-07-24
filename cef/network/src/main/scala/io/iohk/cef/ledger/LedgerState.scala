package io.iohk.cef.ledger

trait LedgerState[K, V] {
  def equivalentTo(that: LedgerState[K, V]): Boolean
  def get(key: K): Option[V]
  def contains(key: K): Boolean
  def put(identity: K, key: V): LedgerState[K, V]
  def remove(identity: K): LedgerState[K, V]
  def keys: Set[K]
}
