package io.iohk.atala.prism.connector

import io.grpc.Status
import io.iohk.atala.prism.connector.model.TokenString
import io.iohk.atala.prism.errors.{PrismError, PrismServerError}

package object errors {

  sealed trait ConnectorError extends PrismError

  case class UnknownValueError(tpe: String, value: String) extends ConnectorError {
    override def toStatus: Status = {
      Status.UNKNOWN.withDescription(s"Unknown $tpe: $value")
    }
  }

  case class InvalidArgumentError(tpe: String, requirement: String, value: String) extends ConnectorError {
    override def toStatus: Status = {
      Status.INVALID_ARGUMENT.withDescription(s"Invalid value for $tpe, expected $requirement, got $value")
    }
  }

  case class NotFoundByFieldError(entityType: String, fieldName: String, fieldValue: String) extends ConnectorError {
    override def toStatus: Status = {
      Status.NOT_FOUND.withDescription(s"""$entityType with $fieldName - "$fieldValue" not found""")
    }
  }

  case class PublicKeyMissingError() extends ConnectorError {
    override def toStatus: Status = {
      Status.UNAUTHENTICATED.withDescription("Authentication required, missing public key")
    }
  }
  case class SignatureMissingError() extends ConnectorError {
    override def toStatus: Status = {
      Status.UNAUTHENTICATED.withDescription("Authentication required, missing signature")
    }
  }
  case class SignatureVerificationError() extends ConnectorError {
    override def toStatus: Status = {
      Status.UNAUTHENTICATED.withDescription("Authentication required, signature invalid")
    }
  }

  final case class InvalidRequest(reason: String) extends ConnectorError {
    def toStatus: Status = Status.INVALID_ARGUMENT.withDescription(reason)
  }

  case class InternalServerError(cause: Throwable) extends ConnectorError with PrismServerError {
    override def toStatus: Status = {
      Status.INTERNAL.withDescription("Internal server error. Please contact administrator.")
    }
  }

  case class ServiceUnavailableError() extends ConnectorError {
    override def toStatus: Status = {
      Status.UNAVAILABLE.withDescription("Service unavailable. Please try later.")
    }
  }

  case class ConnectionNotFound(tokenString: TokenString) extends ConnectorError {
    override def toStatus: Status = {
      Status.NOT_FOUND.withDescription(
        s"Connection with token $tokenString doesn't exist. " +
          s"Other side might not have accepted connection yet or connection token is invalid"
      )
    }
  }
}
