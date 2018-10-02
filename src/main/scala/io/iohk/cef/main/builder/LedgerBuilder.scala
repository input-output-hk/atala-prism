package io.iohk.cef.main.builder
import io.iohk.cef.LedgerId
import io.iohk.cef.ledger.storage.Ledger
import io.iohk.cef.ledger.{ByteStringSerializable, Transaction}
import io.iohk.cef.utils.ForExpressionsEnabler

import scala.language.higherKinds

trait LedgerBuilder[F[_], S, T <: Transaction[S]] {
  self: LedgerStateStorageBuilder[S] with LedgerStorageBuilder with LedgerConfigBuilder =>

  def ledger(id: LedgerId)(
      implicit forExpressionsEnabler: ForExpressionsEnabler[F],
      sByteStringSerializable: ByteStringSerializable[S]): Ledger[F, S] =
    Ledger(id, ledgerStorage, ledgerStateStorage)
}
