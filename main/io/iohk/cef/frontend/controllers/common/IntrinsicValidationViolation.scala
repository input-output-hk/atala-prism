package io.iohk.cef.frontend.controllers.common

import com.alexitc.playsonify.core.I18nService
import com.alexitc.playsonify.models.{GenericPublicError, InputValidationError, PublicError}
import io.iohk.cef.error.ApplicationError

case class IntrinsicValidationViolation(message: String) extends ApplicationError with InputValidationError {
  override def toPublicErrorList[L](i18nService: I18nService[L])(implicit lang: L): List[PublicError] = {
    val msg = i18nService.render(
      s"The provided data failed to satisfy an intrinsic condition over the transaction. Message: ${message}"
    )
    val error = GenericPublicError(msg)
    List(error)
  }
}
