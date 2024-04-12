package io.iohk.atala.prism.node.auth

import derevo.derive
import io.grpc.Status
import io.iohk.atala.prism.node.errors.PrismError
import tofu.logging.derivation.loggable

package object errors {
  @derive(loggable)
  sealed trait AuthError extends PrismError

  final case class SignatureVerificationError() extends AuthError {
    override def toStatus: Status = {
      Status.UNAUTHENTICATED.withDescription("Signature Invalid")
    }
  }

  final case class UnknownPublicKeyId() extends AuthError {
    override def toStatus: Status = {
      Status.UNAUTHENTICATED.withDescription("Unknown public key id")
    }
  }

  case object NoCreateDidOperationError extends AuthError {
    override def toStatus: Status = {
      Status.UNAUTHENTICATED.withDescription(
        "Encoded operation does not create a fresh PrismDid"
      )
    }
  }

  final case class InvalidRequest(reason: String) extends AuthError {
    def toStatus: Status = Status.INVALID_ARGUMENT.withDescription(reason)
  }

}
