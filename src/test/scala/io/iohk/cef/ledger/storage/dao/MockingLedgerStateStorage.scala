package io.iohk.cef.ledger.storage.dao
import io.iohk.cef.ledger.storage.LedgerStateStorage
import org.scalatest.mockito.MockitoSugar

trait MockingLedgerStateStorage[State] {
  self: MockitoSugar =>

  def mockLedgerStateStorage: LedgerStateStorage[State] = mock[LedgerStateStorage[State]]
}
