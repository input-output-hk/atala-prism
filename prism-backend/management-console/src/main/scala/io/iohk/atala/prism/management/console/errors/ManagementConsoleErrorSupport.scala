package io.iohk.atala.prism.management.console.errors

import io.iohk.atala.prism.errors.ErrorSupport

trait ManagementConsoleErrorSupport extends ErrorSupport[ManagementConsoleError, InternalServerError] {
  override def wrapAsServerError(cause: Throwable): InternalServerError =
    InternalServerError(cause)
}
