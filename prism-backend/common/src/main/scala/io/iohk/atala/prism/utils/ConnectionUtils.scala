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
  ): ConnectionIO[Either[PrismError, A]] = {
    (for {
      connectionId <- EitherT.fromOption[ConnectionIO](
        ConnectionId.from(receivedMessage.connectionId).toOption,
        IncorrectConnectionId(receivedMessage)
      )

      connection <-
        EitherT
          .fromOptionF[ConnectionIO, PrismError, A](
            findConnection(connectionId),
            ConnectionDoesNotExist(receivedMessage)
          )
    } yield connection).value
  }

  case class IncorrectConnectionId(receivedMessage: ReceivedMessage) extends PrismError {
    override def toStatus: Status = {
      Status.INTERNAL.withDescription(
        s"Message with id: ${receivedMessage.id} has incorrect connectionId. ${receivedMessage.connectionId} is not valid UUID."
      )
    }
  }

  case class ConnectionDoesNotExist(receivedMessage: ReceivedMessage) extends PrismError {
    override def toStatus: Status = {
      Status.INTERNAL.withDescription(
        s"Message with id: ${receivedMessage.id} and connectionId ${receivedMessage.connectionId}" +
          "does not have corresponding connection in database"
      )
    }
  }

}
