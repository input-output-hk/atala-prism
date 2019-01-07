package io.iohk.query.ledger

import io.iohk.cef.ledger.storage.LedgerStateStorage
import io.iohk.query.QueryEngine

case class LedgerQueryEngine[S](ledgerStateStorage: LedgerStateStorage[S]) extends QueryEngine {
  def get(partitionId: String): Option[S] = ledgerStateStorage.slice(Set(partitionId)).get(partitionId)
  def contains(partitionId: String): Boolean = get(partitionId).isDefined
}
