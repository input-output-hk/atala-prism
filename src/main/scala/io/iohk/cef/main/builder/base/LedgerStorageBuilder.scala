package io.iohk.cef.main.builder.base
import io.iohk.cef.ledger.storage.LedgerStorage

trait LedgerStorageBuilder {
  val ledgerStorage: LedgerStorage
}
