package io.iohk.cef.main.builder
import io.iohk.cef.LedgerId
import io.iohk.cef.ledger.storage.Ledger
import io.iohk.cef.ledger.{ByteStringSerializable, Transaction}

trait LedgerBuilder[S, T <: Transaction[S]] {
  self: LedgerStateStorageBuilder[S] with LedgerStorageBuilder with LedgerConfigBuilder =>

  def ledger(id: LedgerId)(
      implicit
      sByteStringSerializable: ByteStringSerializable[S]): Ledger[S] =
    Ledger(id, ledgerStorage, ledgerStateStorage)
}
