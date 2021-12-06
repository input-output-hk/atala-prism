package io.iohk.atala.prism.vault

import derevo.derive
import io.grpc.Status
import io.iohk.atala.prism.errors.PrismError
import tofu.logging.derivation.loggable

package object errors {
  @derive(loggable)
  sealed trait VaultError extends PrismError

  case class InternalVaultError(cause: Throwable) extends VaultError {
    override def toStatus: Status = {
      Status.INTERNAL.withDescription(
        s"Internal error in the vault service, cause: ${cause.getMessage}. Please contact administrator."
      )
    }
  }

  final case class InvalidRequest(reason: String) extends VaultError {
    def toStatus: Status = Status.INVALID_ARGUMENT.withDescription(reason)
  }
}
