package io.iohk.atala.prism.vault.errors

import io.iohk.atala.prism.errors.ErrorSupport

trait VaultErrorSupport extends ErrorSupport[VaultError] {

  override def wrapAsServerError(cause: Throwable): VaultError = InternalVaultError(cause)

  override def invalidRequest(message: String): VaultError = InvalidRequest(message)
}
