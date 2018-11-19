package io.iohk.cef.ledger.storage.dao
import io.iohk.cef.ledger.storage.LedgerStateStorage
import org.scalatest.mockito.MockitoSugar

trait MockingLedgerStateStorage {
  self: MockitoSugar =>

  def mockLedgerStateStorage: LedgerStateStorage = mock[LedgerStateStorage]
}
