package io.iohk.atala.prism.auth

import io.grpc.Status
import io.iohk.atala.prism.errors.{PrismError, PrismServerError}

package object errors {
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

  case object CanonicalSuffixMatchStateError extends AuthError {
    override def toStatus: Status = {
      Status.UNAUTHENTICATED.withDescription(
        "PrismDid canonical suffix does not match the state content"
      )
    }
  }

  case object InvalidAtalaOperationError extends AuthError {
    override def toStatus: Status = {
      Status.UNAUTHENTICATED.withDescription("Invalid encoded Atala operation")
    }
  }

  case object NoCreateDidOperationError extends AuthError {
    override def toStatus: Status = {
      Status.UNAUTHENTICATED.withDescription(
        "Encoded operation does not create a fresh PrismDid"
      )
    }
  }

  final case class UnsupportedAuthMethod() extends AuthError {
    override def toStatus: Status = {
      Status.UNAUTHENTICATED.withDescription("Unsupported auth method")
    }
  }

  final case class UnexpectedError(status: Status) extends AuthError {
    override def toStatus: Status = status
  }

  final case class InvalidRequest(reason: String) extends AuthError {
    def toStatus: Status = Status.INVALID_ARGUMENT.withDescription(reason)
  }

  final case class InternalServerError(cause: Throwable) extends AuthError with PrismServerError {
    override def toStatus: Status = {
      Status.INTERNAL.withDescription(
        s"Internal server error, cause: ${cause.getMessage}. Please contact administrator."
      )
    }
  }
}
