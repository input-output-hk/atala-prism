package io.iohk.cef.main.builder.derived
import io.iohk.cef.LedgerId
import io.iohk.cef.ledger.Transaction
import io.iohk.cef.ledger.storage.Ledger
import io.iohk.cef.main.builder.base.{LedgerConfigBuilder, LedgerStateStorageBuilder, LedgerStorageBuilder}
import io.iohk.cef.utils.ForExpressionsEnabler

import scala.language.higherKinds

trait LedgerBuilder[F[_], S, T <: Transaction[S]] {
  self: LedgerStateStorageBuilder[S] with LedgerStorageBuilder with LedgerConfigBuilder =>

  def ledger(id: LedgerId)(implicit forExpressionsEnabler: ForExpressionsEnabler[F]) =
    Ledger(id, ledgerStorage, ledgerStateStorage)
}
