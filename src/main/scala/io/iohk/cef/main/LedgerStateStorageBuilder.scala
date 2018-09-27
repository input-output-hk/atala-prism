package io.iohk.cef.main
import io.iohk.cef.crypto._
import io.iohk.cef.ledger.identity.storage.scalike.IdentityLedgerStateStorageImpl
import io.iohk.cef.ledger.identity.storage.scalike.dao.IdentityLedgerStateStorageDao
import io.iohk.cef.ledger.storage.LedgerStateStorage

sealed trait LedgerStateStorageBuilder[S] {
  val ledgerStateStorage: LedgerStateStorage[S]
}

trait IdentityLedgerStateStorageBuilder extends LedgerStateStorageBuilder[Set[SigningPublicKey]] {
  override val ledgerStateStorage: LedgerStateStorage[Set[SigningPublicKey]] =
    new IdentityLedgerStateStorageImpl(new IdentityLedgerStateStorageDao)
}
