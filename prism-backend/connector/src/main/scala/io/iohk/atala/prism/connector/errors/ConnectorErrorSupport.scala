package io.iohk.atala.prism.connector.errors

import io.iohk.atala.prism.errors.ErrorSupport

trait ConnectorErrorSupport extends ErrorSupport[ConnectorError, InternalServerError] {
  override def wrapAsServerError(cause: Throwable): InternalServerError =
    InternalServerError(cause)
}
