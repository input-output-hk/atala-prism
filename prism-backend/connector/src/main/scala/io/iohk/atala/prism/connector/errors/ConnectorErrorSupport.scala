package io.iohk.atala.prism.connector.errors

import io.iohk.atala.prism.errors.{ErrorSupport, LoggingContext, PrismError}

trait ConnectorErrorSupport extends ErrorSupport[ConnectorError] {
  override def wrapAsServerError(cause: Throwable): InternalConnectorError =
    InternalConnectorError(cause)

  override def invalidRequest(message: String): ConnectorError =
    InvalidRequest(message)
}

trait ConnectorErrorSupportNew extends ErrorSupport[ConnectorError] {

  // unused
  override def wrapAsServerError(cause: Throwable): ConnectorError =
    InternalConnectorError(cause)

  // unused
  override def invalidRequest(message: String): ConnectorError = InvalidRequest(
    message
  )

  implicit class ErrorLoggingOpsNew[E <: PrismError](error: E) {
    def logWarnNew(implicit lc: LoggingContext): E = {
      val status = error.toStatus
      logger.warn(
        s"Issuing ${error.getClass.getSimpleName}: ${status.getCode} ${status.getDescription} ($lc)"
      )
      error
    }
  }
}
