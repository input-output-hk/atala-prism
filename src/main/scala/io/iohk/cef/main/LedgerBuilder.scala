package io.iohk.cef.main
import io.iohk.cef.LedgerId
import io.iohk.cef.ledger.Transaction
import io.iohk.cef.ledger.storage.Ledger

import scala.language.higherKinds

trait LedgerBuilder[F[_], S, T <: Transaction[S]] {
  self: LedgerStateStorageBuilder[S] with LedgerStorageBuilder with ForExpressionsEnablerBuilder[F] =>

  implicit val enabler = forExpressionsEnabler

  def ledger(id: LedgerId): Ledger[F, S] = Ledger(id, ledgerStorage, ledgerStateStorage)
}
