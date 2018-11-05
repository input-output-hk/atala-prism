package io.iohk.cef.main.builder
import io.iohk.cef.ledger.storage.LedgerStorage
import io.iohk.cef.ledger.storage.scalike.LedgerStorageImpl
import io.iohk.cef.ledger.storage.scalike.dao.LedgerStorageDao

class LedgerStorageBuilder(ledgerConfigBuilder: LedgerConfigBuilder) {
  import ledgerConfigBuilder._
  val ledgerStorage: LedgerStorage = new LedgerStorageImpl(new LedgerStorageDao(clock))
}
