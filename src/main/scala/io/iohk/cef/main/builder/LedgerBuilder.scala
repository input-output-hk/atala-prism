package io.iohk.cef.main.builder
import io.iohk.cef.LedgerId
import io.iohk.cef.ledger.storage.Ledger
import io.iohk.cef.ledger.{ByteStringSerializable, Transaction}

class LedgerBuilder[S, T <: Transaction[S]](
    ledgerStateStorageBuilder: LedgerStateStorageBuilder[S],
    ledgerStorageBuilder: LedgerStorageBuilder) {

  import ledgerStateStorageBuilder._
  import ledgerStorageBuilder._

  def ledger(id: LedgerId)(
      implicit
      sByteStringSerializable: ByteStringSerializable[S]): Ledger[S] =
    Ledger(id, ledgerStorage, ledgerStateStorage)
}
