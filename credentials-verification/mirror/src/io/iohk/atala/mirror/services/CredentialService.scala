package io.iohk.atala.mirror.services

import cats.data.NonEmptyList
import monix.eval.Task
import doobie.util.transactor.Transactor
import doobie.implicits._
import io.iohk.atala.mirror.db.{ConnectionDao, UserCredentialDao}
import io.iohk.atala.mirror.models.UserCredential
import cats.implicits._
import io.iohk.atala.mirror.models.Connection.ConnectionId
import io.iohk.atala.mirror.models.UserCredential.{MessageId, MessageReceivedDate, RawCredential}
import io.iohk.prism.protos.connector_models.ReceivedMessage
import java.time.Instant

import fs2.Stream
import io.iohk.atala.mirror.Utils.parseUUID
import org.slf4j.LoggerFactory

import scala.concurrent.duration.DurationInt

class CredentialService(tx: Transactor[Task], connectorService: ConnectorClientService) {

  private val logger = LoggerFactory.getLogger(classOf[CredentialService])

  private val GET_MESSAGES_PAGINATED_LIMIT = 100
  private val GET_MESSAGES_PAGINATED_AWAKE_DELAY = 10.seconds

  val credentialUpdatesStream: Stream[Task, Seq[ReceivedMessage]] = {
    Stream
      .eval(UserCredentialDao.findLastSeenMessageId.transact(tx))
      .flatMap { lastSeenMessageId =>
        connectorService.getMessagesPaginatedStream(
          lastSeenMessageId,
          GET_MESSAGES_PAGINATED_LIMIT,
          GET_MESSAGES_PAGINATED_AWAKE_DELAY
        )
      }
      .evalTap(saveMessages)
  }

  private def saveMessages(messages: Seq[ReceivedMessage]): Task[Unit] = {
    val connectionIds = parseConnectionIds(messages)

    for {
      connections <-
        NonEmptyList
          .fromList(connectionIds.toList)
          .map(ids => ConnectionDao.findBy(ids).transact(tx))
          .getOrElse(Task.pure(Nil))

      connectionIdToTokenMap =
        connections
          .flatMap(connection => connection.id.map(_.uuid.toString -> connection.token))
          .toMap

      userCredentials = messages.flatMap { receivedMessage =>
        connectionIdToTokenMap.get(receivedMessage.connectionId) match {
          case Some(token) =>
            Some(
              UserCredential(
                token,
                RawCredential(receivedMessage.message.toString),
                None,
                MessageId(receivedMessage.id),
                MessageReceivedDate(Instant.ofEpochMilli(receivedMessage.received))
              )
            )
          case _ =>
            logger.warn(
              s"Message with id: ${receivedMessage.id} and connectionId ${receivedMessage.connectionId}" +
                s"does not have corresponding connection or connection does not have connectionId, skipping it."
            )
            None
        }
      }.toList

      _ <- UserCredentialDao.insertMany.updateMany(userCredentials).transact(tx)
    } yield ()
  }

  private def parseConnectionIds(messages: Seq[ReceivedMessage]): Seq[ConnectionId] = {
    messages.flatMap { receivedMessage =>
      parseConnectionId(receivedMessage.connectionId).orElse {
        logger.warn(
          s"Message with id: ${receivedMessage.id} has incorrect connectionId. ${receivedMessage.connectionId} " +
            s"is not valid UUID"
        )
        None
      }
    }
  }

  private def parseConnectionId(connectionId: String): Option[ConnectionId] = parseUUID(connectionId).map(ConnectionId)

}
