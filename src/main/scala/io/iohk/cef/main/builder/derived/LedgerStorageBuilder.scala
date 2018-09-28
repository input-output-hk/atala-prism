package io.iohk.cef.main.builder.derived
import io.iohk.cef.ledger.storage.LedgerStorage
import io.iohk.cef.ledger.storage.scalike.LedgerStorageImpl
import io.iohk.cef.ledger.storage.scalike.dao.LedgerStorageDao
import io.iohk.cef.main.builder.base.LedgerConfigBuilder

trait LedgerStorageBuilder {
  self: LedgerConfigBuilder =>
  val ledgerStorage: LedgerStorage = new LedgerStorageImpl(new LedgerStorageDao(clock))
}
