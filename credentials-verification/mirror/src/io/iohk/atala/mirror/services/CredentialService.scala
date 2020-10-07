package io.iohk.atala.mirror.services

import java.time.Instant

import scala.concurrent.duration.DurationInt

import cats.data.NonEmptyList
import monix.eval.Task
import doobie.util.transactor.Transactor
import fs2.Stream
import org.slf4j.LoggerFactory

import io.iohk.prism.protos.connector_models.ReceivedMessage
import io.iohk.atala.mirror.db.{ConnectionDao, UserCredentialDao}
import io.iohk.atala.mirror.models.{Connection, UserCredential, CredentialProofRequestType}
import io.iohk.atala.mirror.models.Connection.{ConnectionId, ConnectionState, ConnectionToken}
import io.iohk.atala.mirror.models.UserCredential.{MessageId, MessageReceivedDate, RawCredential}
import io.iohk.atala.mirror.Utils.parseUUID

import cats.implicits._
import doobie.implicits._

class CredentialService(tx: Transactor[Task], connectorService: ConnectorClientService) {

  private val logger = LoggerFactory.getLogger(classOf[CredentialService])

  private val GET_MESSAGES_PAGINATED_LIMIT = 100
  private val GET_MESSAGES_PAGINATED_AWAKE_DELAY = 10.seconds

  private val GET_CONNECTIONS_PAGINATED_LIMIT = 100
  private val GET_CONNECTIONS_PAGINATED_AWAKE_DELAY = 11.seconds

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

  val connectionUpdatesStream: Stream[Task, Unit] = {
    Stream
      .eval(
        ConnectionDao.findLastSeenConnectionId.transact(tx)
      )
      .flatMap(lastSeenConnectionId =>
        connectorService
          .getConnectionsPaginatedStream(
            lastSeenConnectionId,
            GET_CONNECTIONS_PAGINATED_LIMIT,
            GET_CONNECTIONS_PAGINATED_AWAKE_DELAY
          )
          .evalMap(connectionInfo => {
            val connection = Connection(
              token = ConnectionToken(connectionInfo.token),
              id = parseUUID(connectionInfo.connectionId).map(ConnectionId),
              state = ConnectionState.Connected
            )

            ConnectionDao
              .update(connection)
              .transact(tx)
              .map(_ => connection)
          })
          .evalMap(connection => {
            // TODO: Request credential in this place is temporary and
            //       probably will be removed in the future.
            val credential = CredentialProofRequestType.RedlandIdCredential

            for {
              _ <- connection.id match {
                case None => Task.unit
                case Some(connectionId) =>
                  connectorService
                    .requestCredential(
                      connectionId = connectionId,
                      connectionToken = connection.token,
                      credentialProofRequestTypes = Seq(credential)
                    )
              }

              _ = logger.info(s"Request credential: $credential")
            } yield ()
          })
          .drain // discard return type, as we don't need it
      )
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
