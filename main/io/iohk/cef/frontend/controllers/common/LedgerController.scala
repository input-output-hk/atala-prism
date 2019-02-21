package io.iohk.cef.frontend.controllers.common

import com.alexitc.playsonify.core.ApplicationResult
import io.iohk.cef.frontend.models.UnsupportedLedgerIdError
import io.iohk.cef.frontend.services.LedgerService
import io.iohk.cef.ledger.{LedgerId, Transaction}
import io.iohk.cef.ledger.query.LedgerQuery
import org.scalactic.{Bad, Every, Good}

trait LedgerController {

  def applyLedgerValidation[State, Tx <: Transaction[State], Q <: LedgerQuery[State]](
      ledgerId: LedgerId,
      service: LedgerService[State, Tx, Q]
  ): ApplicationResult[Unit] = {
    if (service.isLedgerSupported(ledgerId)) Good(())
    else Bad(Every(UnsupportedLedgerIdError(ledgerId)))
  }
}
