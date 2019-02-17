package io.iohk.cef.frontend.models

import com.alexitc.playsonify.core.I18nService
import com.alexitc.playsonify.models.{FieldValidationError, InputValidationError, PublicError}
import io.iohk.cef.ledger.LedgerId

final case class UnsupportedLedgerIdError(ledgerId: LedgerId) extends InputValidationError {
  override def toPublicErrorList[L](i18nService: I18nService[L])(implicit lang: L): List[PublicError] = {
    val error = FieldValidationError("ledgerId", s"The provided ledger id ${ledgerId} is not supported.")
    List(error)
  }
}
