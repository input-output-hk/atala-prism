package io.iohk.atala.prism.auth.errors

import io.iohk.atala.prism.errors.ErrorSupport

trait AuthErrorSupport extends ErrorSupport[AuthError] {
  override def wrapAsServerError(cause: Throwable): InternalServerError =
    InternalServerError(cause)

  override def invalidRequest(message: String): AuthError =
    InvalidRequest(message)
}
