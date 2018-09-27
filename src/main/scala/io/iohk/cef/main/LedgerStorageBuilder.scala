package io.iohk.cef.main
import io.iohk.cef.ledger.storage.LedgerStorage

trait LedgerStorageBuilder {
  val ledgerStorage: LedgerStorage
}
