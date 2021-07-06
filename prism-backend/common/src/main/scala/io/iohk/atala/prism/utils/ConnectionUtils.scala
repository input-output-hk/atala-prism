package io.iohk.atala.prism.utils

import cats.data.EitherT
import doobie.ConnectionIO
import io.grpc.Status
import io.iohk.atala.prism.errors.PrismError
import io.iohk.atala.prism.models.ConnectionId
import io.iohk.atala.prism.protos.connector_models.ReceivedMessage

object ConnectionUtils {

  def fromReceivedMessage[A](
      receivedMessage: ReceivedMessage,
      findConnection: ConnectionId => ConnectionIO[Option[A]]
  ): ConnectionIO[Either[PrismError, A]] =
    fromConnectionId(receivedMessage.connectionId, receivedMessage.id, findConnection)

  def fromConnectionId[A](
      connectionId: String,
      receivedMessageId: String,
      findConnection: ConnectionId => ConnectionIO[Option[A]]
  ): ConnectionIO[Either[PrismError, A]] = {
    (for {
      connectionId <- EitherT.fromOption[ConnectionIO](
        ConnectionId.from(connectionId).toOption,
        IncorrectConnectionId(receivedMessageId, connectionId)
      )

      connection <-
        EitherT
          .fromOptionF[ConnectionIO, PrismError, A](
            findConnection(connectionId),
            ConnectionDoesNotExist(receivedMessageId, connectionId.toString)
          )
    } yield connection).value
  }

  case class IncorrectConnectionId(receivedMessageId: String, connectionId: String) extends PrismError {
    override def toStatus: Status = {
      Status.INTERNAL.withDescription(
        s"Message with id: $receivedMessageId has incorrect connectionId. $connectionId is not valid UUID."
      )
    }
  }

  case class ConnectionDoesNotExist(receivedMessageId: String, connectionId: String) extends PrismError {
    override def toStatus: Status = {
      Status.INTERNAL.withDescription(
        s"Message with id: $receivedMessageId and connectionId $connectionId" +
          "does not have corresponding connection in database"
      )
    }
  }

}
