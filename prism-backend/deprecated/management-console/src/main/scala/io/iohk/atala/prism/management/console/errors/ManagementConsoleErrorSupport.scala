package io.iohk.atala.prism.management.console.errors

import io.iohk.atala.prism.errors.ErrorSupport

trait ManagementConsoleErrorSupport extends ErrorSupport[ManagementConsoleError] {
  override def wrapAsServerError(cause: Throwable): InternalServerError =
    InternalServerError(cause)

  override def invalidRequest(message: String): ManagementConsoleError =
    InvalidRequest(message)
}
