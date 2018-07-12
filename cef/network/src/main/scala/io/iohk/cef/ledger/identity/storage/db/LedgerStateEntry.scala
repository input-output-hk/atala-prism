package io.iohk.cef.ledger.identity.storage.db

case class LedgerStateEntry[I, K](identity: I, key: K) {
  def toEntryMap =
    LedgerStateEntryMap[I, K](identity, Set(key))
}

case class LedgerStateEntryMap[I, K](map: Map[I, Set[K]] = Map[I, Set[K]]()) {
  def aggregateWith(entry: LedgerStateEntry[I, K]) = {
    LedgerStateEntryMap[I, K](
      map + ((entry.identity, map.get(entry.identity).getOrElse(Set()) + entry.key))
    )
  }
}

object LedgerStateEntryMap {
  def apply[I, K](identity: I, keys: Set[K]): LedgerStateEntryMap[I, K] =
    LedgerStateEntryMap(Map((identity -> keys)))
}
