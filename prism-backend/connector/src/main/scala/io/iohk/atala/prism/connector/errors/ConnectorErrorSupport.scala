package io.iohk.atala.prism.connector.errors

import io.iohk.atala.prism.errors.{ErrorSupport, LoggingContext, PrismError}

trait ConnectorErrorSupport extends ErrorSupport[ConnectorError] {
  override def wrapAsServerError(cause: Throwable): InternalServerError =
    InternalServerError(cause)

  override def invalidRequest(message: String): ConnectorError =
    InvalidRequest(message)
}

trait ConnectorErrorSupportNew extends ErrorSupport[ConnectorError] {

  override def wrapAsServerError(cause: Throwable): ConnectorError = InternalServerError(cause)

  override def invalidRequest(message: String): ConnectorError = InvalidRequest(message)

  def invalidArgumentError[
    E <: PrismError
    : InvalidArgumentError <:< *](tpe: String, requirement: String, value: String): E = InvalidArgumentError(tpe, requirement, value)

  implicit class ErrorLoggingOpsNew[E <: PrismError](error: E) {
    def logWarnNew(implicit lc: LoggingContext): E = {
      val status = error.toStatus
      logger.warn(s"Issuing ${error.getClass.getSimpleName}: ${status.getCode} ${status.getDescription} ($lc)")
      error
    }
  }
}
