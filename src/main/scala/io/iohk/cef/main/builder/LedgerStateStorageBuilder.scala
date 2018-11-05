package io.iohk.cef.main.builder

import io.iohk.cef.crypto._
import io.iohk.cef.ledger.identity.storage.scalike.IdentityLedgerStateStorageImpl
import io.iohk.cef.ledger.identity.storage.scalike.dao.IdentityLedgerStateStorageDao
import io.iohk.cef.ledger.storage.LedgerStateStorage
import io.iohk.cef.ledger.storage.scalike.LedgerStateStorageImpl
import io.iohk.cef.ledger.storage.scalike.dao.LedgerStateStorageDao
import io.iohk.cef.codecs.nio._

sealed trait LedgerStateStorageBuilder[S] {
  def ledgerStateStorage(implicit byteStringSerializable: NioEncDec[S]): LedgerStateStorage[S]
}

class DefaultLedgerStateStorageBuilder[S](ledgerConfigBuilder: LedgerConfigBuilder)
    extends LedgerStateStorageBuilder[S] {
  import ledgerConfigBuilder._
  override def ledgerStateStorage(implicit byteStringSerializable: NioEncDec[S]): LedgerStateStorage[S] = {
    new LedgerStateStorageImpl[S](ledgerConfig.id, new LedgerStateStorageDao)
  }
}

class IdentityLedgerStateStorageBuilder extends LedgerStateStorageBuilder[Set[SigningPublicKey]] {
  override def ledgerStateStorage(
      implicit byteStringSerializable: NioEncDec[Set[SigningPublicKey]]): LedgerStateStorage[Set[SigningPublicKey]] =
    new IdentityLedgerStateStorageImpl(new IdentityLedgerStateStorageDao)
}
