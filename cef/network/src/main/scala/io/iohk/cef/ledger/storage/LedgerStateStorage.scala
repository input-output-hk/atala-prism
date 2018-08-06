package io.iohk.cef.ledger.storage

trait LedgerStateStorage[S] {

  def slice(keys: Set[String]): Partitioned[S]

  def update(previousState: Partitioned[S], newState: Partitioned[S]): Unit
}
