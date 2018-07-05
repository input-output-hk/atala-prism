package io.iohk.cef.ledger.persistence.identity

case class LedgerStateEntry[I, K](identity: I, key: K) {
  def toAggregatedEntry = new LedgerStateAggregatedEntry[I, K](identity, Set(key))
}

case class LedgerStateAggregatedEntry[I, K](identity: I, keys: Set[K]) {
  def aggregate(entry: LedgerStateEntry[I, K]) = {
    require(entry.identity == identity)
    new LedgerStateAggregatedEntry[I, K](identity, keys + entry.key)
  }
}
