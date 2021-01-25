package io.iohk.atala.prism.kycbridge.models

import java.time.Instant

import cats.data.EitherT
import doobie.ConnectionIO
import io.iohk.atala.prism.models.{ConnectionId, ConnectionState, ConnectionToken}
import io.iohk.atala.prism.kycbridge.models.Connection.AcuantDocumentInstanceId
import io.iohk.atala.prism.kycbridge.models.assureId.DocumentStatus
import io.iohk.atala.prism.protos.connector_models.ReceivedMessage
import io.iohk.atala.prism.services.MessageProcessor.MessageProcessorException
import io.iohk.atala.prism.kycbridge.db.ConnectionDao

case class Connection(
    token: ConnectionToken,
    id: Option[ConnectionId],
    state: ConnectionState,
    updatedAt: Instant = Instant.now(),
    acuantDocumentInstanceId: Option[AcuantDocumentInstanceId],
    acuantDocumentStatus: Option[DocumentStatus]
)

object Connection {
  case class AcuantDocumentInstanceId(id: String) extends AnyVal

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
