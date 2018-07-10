package io.iohk.cef.ledger.storage.db

case class LedgerStateEntry[I, K](identity: I, key: K) {
  def toAggregatedEntry =
    LedgerStateAggregatedEntries[I, K](identity, Set(key))
}

case class LedgerStateAggregatedEntries[I, K](map: Map[I, Set[K]] = Map[I, Set[K]]()) {
  def aggregate(entry: LedgerStateEntry[I, K]) = {
    LedgerStateAggregatedEntries[I, K](
      map + ((entry.identity, map.get(entry.identity).getOrElse(Set()) + entry.key))
    )
  }
}

object LedgerStateAggregatedEntries {
  def apply[I, K](identity: I, keys: Set[K]): LedgerStateAggregatedEntries[I, K] =
    LedgerStateAggregatedEntries(Map((identity -> keys)))
}
