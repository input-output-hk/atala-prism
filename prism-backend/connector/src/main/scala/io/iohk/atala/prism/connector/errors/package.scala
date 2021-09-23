package io.iohk.atala.prism.connector

import derevo.derive
import io.grpc.Status
import io.iohk.atala.prism.connector.model.{ConnectionId, MessageId, TokenString}
import io.iohk.atala.prism.errors.{PrismError, PrismServerError}
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.crypto.keys.ECPublicKey
import io.iohk.atala.prism.identity.{PrismDid => DID}
import tofu.logging.{DictLoggable, LogRenderer, Loggable}
import io.iohk.atala.prism.logging.GeneralLoggableInstances._
import tofu.logging.derivation.loggable
import tofu.syntax.loggable.TofuLoggableOps

package object errors {

  @derive(loggable)
  sealed trait ConnectorError extends PrismError

  case class UnknownValueError(tpe: String, value: String) extends ConnectorError {
    override def toStatus: Status = {
      Status.UNKNOWN.withDescription(s"Unknown $tpe: $value")
    }
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

  case class ConnectionNotFound(connection: Either[TokenString, ConnectionId]) extends ConnectorError {
    override def toStatus: Status = {
      Status.NOT_FOUND.withDescription(
        s"Connection with ${connection.fold("token " + _, "id " + _)} doesn't exist. " +
          s"Other side might not have accepted connection yet or connection token is invalid"
      )
    }
  }

  object ConnectionNotFound {

    implicit val connectionNotFoundLoggable: Loggable[ConnectionNotFound] = new DictLoggable[ConnectionNotFound] {
      override def fields[I, V, R, S](a: ConnectionNotFound, i: I)(implicit r: LogRenderer[I, V, R, S]): R =
        r.addString("ConnectionNotFound", a.connection.fold(_.logShow, _.logShow), i)

      override def logShow(a: ConnectionNotFound): String =
        s"{ConnectionNotFound=${a.connection.fold(_.logShow, _.logShow)})}"
    }

    def apply(s: TokenString): ConnectionNotFound = ConnectionNotFound(Left(s))
    def apply(c: ConnectionId): ConnectionNotFound = ConnectionNotFound(Right(c))
  }

  case class ConnectionRevoked(connection: Either[TokenString, ConnectionId]) extends ConnectorError {
    override def toStatus: Status = {
      Status.FAILED_PRECONDITION.withDescription(
        s"Connection with ${connection.fold("token " + _, "id " + _)} has been revoked."
      )
    }
  }

  object ConnectionRevoked {

    implicit val connectionRevokedLoggable: Loggable[ConnectionRevoked] = new DictLoggable[ConnectionRevoked] {
      override def fields[I, V, R, S](a: ConnectionRevoked, i: I)(implicit r: LogRenderer[I, V, R, S]): R =
        r.addString("ConnectionRevoked", a.connection.fold(_.logShow, _.logShow), i)

      override def logShow(a: ConnectionRevoked): String =
        s"{ConnectionRevoked=${a.connection.fold(_.logShow, _.logShow)})}"
    }

    def apply(s: TokenString): ConnectionRevoked = ConnectionRevoked(Left(s))
    def apply(c: ConnectionId): ConnectionRevoked = ConnectionRevoked(Right(c))
  }

  case class ConnectionNotFoundByConnectionIdAndSender(sender: ParticipantId, connection: ConnectionId)
      extends ConnectorError {
    override def toStatus: Status = {
      Status.NOT_FOUND.withDescription(
        s"Failed to send message, the connection $connection with sender $sender doesn't exist"
      )
    }
  }

  case class MessagesAlreadyExist(ids: List[MessageId]) extends ConnectorError {
    override def toStatus: Status = {
      Status.ALREADY_EXISTS.withDescription(
        s"Messages with provided ids already exist: ${ids.map(_.uuid.toString).mkString(", ")}"
      )
    }
  }

  case class MessageIdsNotUnique(ids: List[MessageId]) extends ConnectorError {
    override def toStatus: Status = {
      Status.ALREADY_EXISTS.withDescription(
        s"All user provided messages ids must be unique, duplicates: ${ids.map(_.uuid.toString).mkString(", ")}"
      )
    }
  }
}
