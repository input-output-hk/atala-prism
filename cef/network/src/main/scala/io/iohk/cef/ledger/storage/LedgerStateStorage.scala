package io.iohk.cef.ledger.storage

import io.iohk.cef.ledger.Partitioned

trait LedgerStateStorage[S] {

  def slice(keys: Set[String]): Partitioned[S]

  def update(previousState: Partitioned[S], newState: Partitioned[S]): Unit
}
