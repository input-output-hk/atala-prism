package io.iohk.atala.prism.connector

import io.grpc.Status
import io.iohk.atala.prism.connector.model.{ConnectionId, MessageId, TokenString}
import io.iohk.atala.prism.errors.{PrismError, PrismServerError}
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.kotlin.crypto.keys.ECPublicKey
import io.iohk.atala.prism.kotlin.identity.DID

package object errors {

  sealed trait ConnectorError extends PrismError

  case class UnknownValueError(tpe: String, value: String) extends ConnectorError {
    override def toStatus: Status = {
      Status.UNKNOWN.withDescription(s"Unknown $tpe: $value")
    }
  }

  class InvalidArgumentError(val tpe: String, val requirement: String, val value: String) extends ConnectorError {
    override def toStatus: Status = {
      Status.INVALID_ARGUMENT.withDescription(s"Invalid value for $tpe, expected $requirement, got $value")
    }
  }

  object InvalidArgumentError {
    def apply(tpe: String, requirement: String, value: String): InvalidArgumentError =
      new InvalidArgumentError(tpe, requirement, value)
  }

  case class DidConnectionExist(did: DID) extends ConnectorError {
    override def toStatus: Status = {
      Status.ALREADY_EXISTS.withDescription(
        s"Attempting to accept a connection with a DID: $did. DID is already used for a connection, use a different DID"
      )
    }
  }
  case class PkConnectionExist(pk: ECPublicKey) extends ConnectorError {
    override def toStatus: Status = {
      Status.ALREADY_EXISTS.withDescription(
        s"Attempting to accept a connection with a public key: $pk. Public key is already used for a connection, use a different Public key"
      )
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

  case class InternalConnectorError(cause: Throwable) extends ConnectorError {
    override def toStatus: Status = {
      Status.INTERNAL.withDescription("Internal error in the connector service. Please contact administrator.")
    }
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

  trait MessagesError extends ConnectorError

  object MessagesError {
    class ConnectionNotFound(val connection: Either[TokenString, ConnectionId]) extends MessagesError {
      override def toStatus: Status = {
        Status.NOT_FOUND.withDescription(
          s"Connection with ${connection.fold("token " + _, "id " + _)} doesn't exist. " +
            s"Other side might not have accepted connection yet or connection token is invalid"
        )
      }
    }

    class ConnectionRevoked(val connection: Either[TokenString, ConnectionId]) extends MessagesError {
      override def toStatus: Status = {
        Status.FAILED_PRECONDITION.withDescription(
          s"Connection with ${connection.fold("token " + _, "id " + _)} has been revoked."
        )
      }
    }

    class ConnectionNotFoundByConnectionIdAndSender(val sender: ParticipantId, val connection: ConnectionId)
      extends MessagesError {
      override def toStatus: Status = {
        Status.NOT_FOUND.withDescription(
          s"Failed to send message, the connection $connection with sender $sender doesn't exist"
        )
      }
    }

    class MessagesAlreadyExist(val ids: List[MessageId]) extends MessagesError {
      override def toStatus: Status = {
        Status.ALREADY_EXISTS.withDescription(
          s"Messages with provided ids already exist: ${ids.map(_.uuid.toString).mkString(", ")}"
        )
      }
    }

    class MessageIdsNotUnique(val ids: List[MessageId]) extends MessagesError {
      override def toStatus: Status = {
        Status.ALREADY_EXISTS.withDescription(
          s"All user provided messages ids must be unique, duplicates: ${ids.map(_.uuid.toString).mkString(", ")}"
        )
      }
    }
  }
}
