package io.iohk.cef.query.ledger

import io.iohk.cef.ledger.storage.LedgerStateStorage
import io.iohk.cef.query.QueryEngine

case class LedgerQueryEngine[S](private val ledgerStateStorage: LedgerStateStorage[S]) extends QueryEngine {
  def get(partitionId: String): Option[S] = ledgerStateStorage.slice(Set(partitionId)).get(partitionId)
  def contains(partitionId: String): Boolean = get(partitionId).isDefined
  def keys(): Set[String] = ledgerStateStorage.keys
}
