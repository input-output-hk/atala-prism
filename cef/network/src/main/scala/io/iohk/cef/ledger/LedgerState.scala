package io.iohk.cef.ledger

case class LedgerState[S](map: Map[String, S]) {
  def get(key: String): Option[S] = map.get(key)
  def contains(key: String): Boolean = map.contains(key)
  def put(key: String, value: S): LedgerState[S] = LedgerState(map + ((key, value)))
  def remove(key: String): LedgerState[S] = LedgerState(map - key)
  def keys: Set[String] = map.keySet
}
