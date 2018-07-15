package io.iohk.cef.ledger

case class LedgerState[K, V](map: Map[K, V]) {
  def get(key: K): Option[V] = map.get(key)
  def contains(key: K): Boolean = map.contains(key)
  def put(key: K, value: V): LedgerState[K,V] = LedgerState(map + ((key, value)))
  def remove(identity: K): LedgerState[K,V] = LedgerState(map - identity)
  def keys: Set[K] = map.keySet
}
