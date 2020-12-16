package io.iohk.atala.mirror.utils

import cats.free.Free
import doobie.ConnectionIO
import io.iohk.atala.prism.utils.UUIDUtils.parseUUID
import io.iohk.atala.mirror.db.ConnectionDao
import io.iohk.atala.prism.models.ConnectionId
import io.iohk.atala.prism.protos.connector_models.ReceivedMessage
import org.slf4j.Logger
import io.iohk.atala.mirror.models.Connection

object ConnectionUtils {

  def findConnection(
      receivedMessage: ReceivedMessage,
      logger: Logger
  ): ConnectionIO[Option[Connection]] = {
    parseConnectionId(receivedMessage.connectionId)
      .orElse {
        logger.warn(
          s"Message with id: ${receivedMessage.id} has incorrect connectionId. ${receivedMessage.connectionId} " +
            s"is not valid UUID"
        )
        None
      }
      .map(ConnectionDao.findByConnectionId(_))
      .getOrElse {
        logger.warn(
          s"Message with id: ${receivedMessage.id} and connectionId ${receivedMessage.connectionId}" +
            "does not have corresponding connection or connection does not have connectionId, skipping it."
        )
        Free.pure(None)
      }
  }

  def parseConnectionId(connectionId: String): Option[ConnectionId] =
    parseUUID(connectionId).map(ConnectionId)

}
