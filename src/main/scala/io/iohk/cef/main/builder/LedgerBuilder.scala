package io.iohk.cef.main.builder
import io.iohk.cef.LedgerId
import io.iohk.cef.ledger.storage.Ledger
import io.iohk.cef.ledger.Transaction
import io.iohk.cef.codecs.nio._

class LedgerBuilder[S, T <: Transaction[S]](
    ledgerStateStorageBuilder: LedgerStateStorageBuilder[S],
    ledgerStorageBuilder: LedgerStorageBuilder) {

  import ledgerStateStorageBuilder._
  import ledgerStorageBuilder._

  def ledger(id: LedgerId)(
      implicit
      sNioEncDec: NioEncDec[S]): Ledger[S] =
    Ledger(id, ledgerStorage, ledgerStateStorage)
}
