package io.iohk.atala.mirror.models

import java.time.Instant

import cats.data.EitherT
import doobie.ConnectionIO
import io.iohk.atala.mirror.models.Connection._
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.models.{ConnectionId, ConnectionState, ConnectionToken}
import io.iohk.atala.prism.services.MessageProcessor.MessageProcessorException
import io.iohk.atala.prism.protos.connector_models.ReceivedMessage
import io.iohk.atala.mirror.db.ConnectionDao

case class Connection(
    token: ConnectionToken,
    id: Option[ConnectionId],
    state: ConnectionState,
    updatedAt: Instant = Instant.now(),
    holderDID: Option[DID],
    payIdName: Option[PayIdName]
)

object Connection {
  case class PayIdName(name: String) extends AnyVal

  def fromReceivedMessage(
      receivedMessage: ReceivedMessage
  ): ConnectionIO[Either[MessageProcessorException, Connection]] = {
    (for {
      connectionId <- EitherT.fromOption[ConnectionIO](
        ConnectionId.from(receivedMessage.connectionId).toOption,
        MessageProcessorException(
          s"Message with id: ${receivedMessage.id} has incorrect connectionId. ${receivedMessage.connectionId} is not valid UUID."
        )
      )

      connection <-
        EitherT
          .fromOptionF(
            ConnectionDao.findByConnectionId(connectionId),
            MessageProcessorException(
              s"Message with id: ${receivedMessage.id} and connectionId ${receivedMessage.connectionId}" +
                "does not have corresponding connection or connection does not have connectionId, skipping it."
            )
          )
    } yield connection).value
  }
}
