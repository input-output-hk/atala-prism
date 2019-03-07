package io.iohk.cef.frontend.controllers

import com.alexitc.playsonify.core.{ApplicationResult, FutureApplicationResult, I18nService}
import com.alexitc.playsonify.models.{
  FieldValidationError,
  GenericPublicError,
  InputValidationError,
  PublicError,
  ApplicationError => PlaysonifyError
}
import io.iohk.cef.error.{ApplicationError => CefError}
import io.iohk.cef.frontend.client
import io.iohk.cef.frontend.services.IdentityTransactionService
import io.iohk.cef.ledger.UnsupportedLedgerException
import org.scalactic.{Bad, Good}

import scala.concurrent.ExecutionContext

package object common {

  val exceptionHandler: PartialFunction[Throwable, PlaysonifyError] = {
    case _: UnsupportedLedgerException =>
      val error = fieldValidationError("ledgerId", "Unsupported ledger")
      error
  }

  def handlingException[T](
      futureApplicationResult: FutureApplicationResult[T]
  )(implicit ec: ExecutionContext): FutureApplicationResult[T] = {

    futureApplicationResult
      .recover {
        case ex: Throwable if exceptionHandler.isDefinedAt(ex) =>
          val error = exceptionHandler.apply(ex)
          Bad(error).accumulating
      }
  }

  def fromFutureEither[T](value: client.Response[T], playsonifyError: PlaysonifyError)(
      implicit ec: ExecutionContext
  ): FutureApplicationResult[T] = {

    value
      .map { fromEither(_, playsonifyError) }
      .recover {
        case ex: Throwable if exceptionHandler.isDefinedAt(ex) =>
          val error = exceptionHandler.apply(ex)
          Bad(error).accumulating
      }
  }

  def fromEither[T](value: Either[CefError, T], playsonifyError: PlaysonifyError): ApplicationResult[T] = value match {
    case Left(e: IntrinsicValidationViolation) => Bad(e).accumulating
    case Left(x: IdentityTransactionService.CorrespondingPrivateKeyRequiredForLinkingIdentityError.type) =>
      Bad(genericError(x.toString)).accumulating
    case Left(x: IdentityTransactionService.CorrespondingSignatureRequiredForLinkingIdentityError.type) =>
      Bad(genericError(x.toString)).accumulating
    case Left(_) => Bad(playsonifyError).accumulating
    case Right(result) => Good(result)
  }

  private def genericError(message: String): InputValidationError = {
    new InputValidationError {
      override def toPublicErrorList[L](i18nService: I18nService[L])(implicit lang: L): List[PublicError] = {
        List(GenericPublicError(message))
      }
    }
  }

  private def fieldValidationError(field: String, message: String): InputValidationError = {
    new InputValidationError {
      override def toPublicErrorList[L](i18nService: I18nService[L])(implicit lang: L): List[PublicError] = {
        List(FieldValidationError(field, message))
      }
    }
  }
}
