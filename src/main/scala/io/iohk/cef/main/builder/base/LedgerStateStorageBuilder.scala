package io.iohk.cef.main.builder.base

import io.iohk.cef.crypto._
import io.iohk.cef.ledger.ByteStringSerializable
import io.iohk.cef.ledger.identity.storage.scalike.IdentityLedgerStateStorageImpl
import io.iohk.cef.ledger.identity.storage.scalike.dao.IdentityLedgerStateStorageDao
import io.iohk.cef.ledger.storage.LedgerStateStorage
import io.iohk.cef.ledger.storage.scalike.LedgerStateStorageImpl
import io.iohk.cef.ledger.storage.scalike.dao.LedgerStateStorageDao

sealed trait LedgerStateStorageBuilder[S] {
  def ledgerStateStorage(implicit byteStringSerializable: ByteStringSerializable[S]): LedgerStateStorage[S]
}

trait DefaultLedgerStateStorageBuilder[S] extends LedgerStateStorageBuilder[S] {
  self: LedgerConfigBuilder =>
  override def ledgerStateStorage(implicit byteStringSerializable: ByteStringSerializable[S]): LedgerStateStorage[S] = {
    new LedgerStateStorageImpl[S](ledgerConfig.id, new LedgerStateStorageDao)
  }
}

trait IdentityLedgerStateStorageBuilder extends LedgerStateStorageBuilder[Set[SigningPublicKey]] {
  override def ledgerStateStorage(implicit byteStringSerializable: ByteStringSerializable[Set[SigningPublicKey]])
    : LedgerStateStorage[Set[SigningPublicKey]] =
    new IdentityLedgerStateStorageImpl(new IdentityLedgerStateStorageDao)
}
