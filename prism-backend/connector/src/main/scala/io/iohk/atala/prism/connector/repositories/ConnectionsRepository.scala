package io.iohk.atala.prism.connector.repositories

import cats.data.{EitherT, OptionT}
import cats.effect.IO
import cats.implicits.catsSyntaxEitherId
import cats.syntax.functor._
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.crypto.ECPublicKey
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither._
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import io.iohk.atala.prism.connector.errors._
import io.iohk.atala.prism.connector.model._
import io.iohk.atala.prism.connector.repositories.daos.{
  ConnectionTokensDAO,
  ConnectionsDAO,
  MessagesDAO,
  ParticipantsDAO
}
import io.iohk.atala.prism.console.models.Institution
import io.iohk.atala.prism.console.repositories.daos.ContactsDAO
import io.iohk.atala.prism.errors.LoggingContext
import io.iohk.atala.prism.identity.DID
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext

trait ConnectionsRepository {

  def insertTokens(initiator: ParticipantId, tokens: List[TokenString]): FutureEither[Nothing, List[TokenString]]

  def getTokenInfo(token: TokenString): FutureEither[ConnectorError, ParticipantInfo]

  def addConnectionFromToken(
      token: TokenString,
      didOrPublicKey: Either[DID, ECPublicKey]
  ): FutureEither[ConnectorError, (ParticipantId, ConnectionInfo)]

  def revokeConnection(participantId: ParticipantId, connectionId: ConnectionId): FutureEither[ConnectorError, Unit]

  def getConnectionsPaginated(
      participant: ParticipantId,
      limit: Int,
      lastSeenConnectionId: Option[ConnectionId]
  ): FutureEither[ConnectorError, Seq[ConnectionInfo]]

  def getOtherSideInfo(
      id: ConnectionId,
      participant: ParticipantId
  ): FutureEither[ConnectorError, Option[ParticipantInfo]]

  def getConnectionByToken(token: TokenString): FutureEither[ConnectorError, Option[Connection]]

  def getConnectionsByConnectionTokens(
      connectionTokens: List[TokenString]
  ): FutureEither[ConnectorError, List[ContactConnection]]
}

object ConnectionsRepository {
  class PostgresImpl(xa: Transactor[IO])(implicit ec: ExecutionContext)
      extends ConnectionsRepository
      with ConnectorErrorSupport {
    val logger: Logger = LoggerFactory.getLogger(getClass)

    override def insertTokens(
        initiator: ParticipantId,
        tokens: List[TokenString]
    ): FutureEither[Nothing, List[TokenString]] = {
      ConnectionTokensDAO
        .insert(initiator, tokens)
        .logSQLErrors("inserting tokens", logger)
        .transact(xa)
        .unsafeToFuture()
        .as(Right(tokens))
        .toFutureEither
    }

    override def getTokenInfo(token: TokenString): FutureEither[ConnectorError, ParticipantInfo] = {
      implicit val loggingContext = LoggingContext("token" -> token)

      ParticipantsDAO
        .findBy(token)
        .toRight(UnknownValueError("token", token.token).logWarn)
        .value
        .logSQLErrors("getting token info", logger)
        .transact(xa)
        .unsafeToFuture()
        .toFutureEither
    }

    override def addConnectionFromToken(
        token: TokenString,
        didOrPublicKey: Either[DID, ECPublicKey]
    ): FutureEither[ConnectorError, (ParticipantId, ConnectionInfo)] = {

      val maybeDid = didOrPublicKey.left.toOption
      val maybePublicKey = didOrPublicKey.toOption

      implicit val loggingContext: LoggingContext =
        LoggingContext("token" -> token, "did" -> maybeDid, "publicKey" -> maybePublicKey)

      // if exist Left(ConnectorError) else Right(Unit)
      val checkIfAlreadyExist: EitherT[doobie.ConnectionIO, ConnectorError, Unit] = (maybeDid, maybePublicKey) match {
        case (Some(did), None) =>
          EitherT {
            ParticipantsDAO.findByDID(did).value.map {
              case None => Right(())
              case Some(_) => Left(DidConnectionExist(did))
            }
          }
        case (None, Some(pk)) =>
          EitherT {
            ParticipantsDAO.findByPublicKey(pk).value.map {
              case None => Right(())
              case Some(_) => Left(PkConnectionExist(pk))
            }
          }
        case _ => // this is the case for (None, None), (Some(_), Some(_) can not happen, at least pk or did must be used for auth)
          EitherT.leftT[doobie.ConnectionIO, Unit](
            InvalidRequest(
              s"Expected either DID or Public key, got neither"
            )
          )
      }

      val query = for {
        initiator <-
          ParticipantsDAO
            .findByAvailableToken(token)
            .toRight(UnknownValueError("token", token.token).logWarn)

        _ <- checkIfAlreadyExist

        // Create a holder, which has no name, instead it has did or a public key
        acceptorInfo = ParticipantInfo(
          id = ParticipantId.random(),
          tpe = ParticipantType.Holder,
          publicKey = maybePublicKey,
          name = "",
          did = maybeDid,
          logo = None,
          operationId = None
        )

        _ <- EitherT.right[ConnectorError] {
          ParticipantsDAO.insert(acceptorInfo)
        }

        ciia <- EitherT.right[ConnectorError](
          ConnectionsDAO.insert(
            initiator = initiator.id,
            acceptor = acceptorInfo.id,
            token = token,
            connectionStatus = ConnectionStatus.ConnectionAccepted
          )
        )
        (connectionId, instantiatedAt) = ciia

        // Kept for the sake of backward compatibility with the existing management console
        // TODO: Remove it when management console is fully decoupled from connector
        _ <- EitherT.right[ConnectorError] {
          ContactsDAO.setConnectionAsAccepted(
            Institution.Id(initiator.id.uuid),
            token,
            connectionId
          )
        }

        _ <- EitherT.right[ConnectorError](ConnectionTokensDAO.markAsUsed(token))
      } yield acceptorInfo.id -> ConnectionInfo(
        connectionId,
        instantiatedAt,
        initiator,
        token,
        ConnectionStatus.ConnectionAccepted
      )

      query.value
        .logSQLErrors("adding connection from token", logger)
        .transact(xa)
        .unsafeToFuture()
        .toFutureEither
    }

    override def revokeConnection(
        participantId: ParticipantId,
        connectionId: ConnectionId
    ): FutureEither[ConnectorError, Unit] = {
      // verify the connection belongs to the participant, and its connected
      def verifyOwnership = {
        for {
          connectionMaybe <- EitherT(ConnectionsDAO.getRawConnection(connectionId).map(_.asRight[ConnectorError]))
          resultE = connectionMaybe match {
            // The connection can't be revoked when the participant is not involved in the connection,
            // or the connection is not established
            case Some(connection)
                if connection.contains(participantId) && connection.status == ConnectionStatus.ConnectionAccepted =>
              doobie.free.connection.pure(().asRight[ConnectorError])
            case _ =>
              val error: ConnectorError = UnknownValueError("connectionId", connectionId.uuid.toString)
              doobie.free.connection.pure(error.asLeft[Unit])
          }
          _ <- EitherT(resultE)
        } yield ()
      }

      val query = for {
        _ <- verifyOwnership
        affectedRows <- EitherT {
          ConnectionsDAO.revoke(connectionId).map(_.asRight[ConnectorError])
        }
        resultE = {
          if (affectedRows == 1) {
            doobie.free.connection.pure(().asRight[ConnectorError])
          } else {
            val error: ConnectorError = InternalServerError(
              new RuntimeException("Unable to revoke the connection, please try again later")
            )
            doobie.free.connection.pure(error.asLeft[Unit])
          }
        }
        _ <- EitherT(resultE)
        // TODO: Remove it when management console is fully decoupled from connector
        _ <- EitherT.right[ConnectorError] {
          ContactsDAO.setConnectionAsRevoked(connectionId).map(_.asRight[ConnectorError])
        }
        // TODO: Remove once messages are being removed after they are read
        _ <- EitherT {
          MessagesDAO.deleteConnectionMessages(connectionId).map(_.asRight[ConnectorError])
        }
      } yield ()

      query.value
        .logSQLErrors(s"revoke connection, connection id - $connectionId", logger)
        .transact(xa)
        .unsafeToFuture()
        .toFutureEither
    }

    override def getConnectionsPaginated(
        participant: ParticipantId,
        limit: Int,
        lastSeenConnectionId: Option[ConnectionId]
    ): FutureEither[ConnectorError, Seq[ConnectionInfo]] = {
      implicit val loggingContext = LoggingContext("participant" -> participant)

      if (limit <= 0) {
        Left(InvalidArgumentError("limit", "positive value", limit.toString).logWarn).toFutureEither
      } else {
        ConnectionsDAO
          .getConnectionsPaginated(participant, limit, lastSeenConnectionId)
          .logSQLErrors("getting connection paginated", logger)
          .transact(xa)
          .unsafeToFuture()
          .map(seq => Right(seq))
          .toFutureEither
      }
    }

    override def getOtherSideInfo(
        id: ConnectionId,
        userId: ParticipantId
    ): FutureEither[ConnectorError, Option[ParticipantInfo]] = {
      val query = for {
        participantId <- OptionT(ConnectionsDAO.getOtherSide(id, userId))
        participantInfo <- ParticipantsDAO.findBy(participantId)
      } yield participantInfo

      query.value
        .logSQLErrors(s"getting other side info, connection id - $id", logger)
        .transact(xa)
        .unsafeToFuture()
        .map(Right(_))
        .toFutureEither
    }

    override def getConnectionByToken(token: TokenString): FutureEither[ConnectorError, Option[Connection]] = {
      ConnectionsDAO
        .getConnectionByToken(token)
        .logSQLErrors("getting connection by token", logger)
        .transact(xa)
        .unsafeToFuture()
        .map(Right(_))
        .toFutureEither
    }

    override def getConnectionsByConnectionTokens(
        connectionTokens: List[TokenString]
    ): FutureEither[ConnectorError, List[ContactConnection]] = {
      ConnectionsDAO
        .getConnectionsByConnectionTokens(connectionTokens)
        .logSQLErrors("getting by connection tokens", logger)
        .transact(xa)
        .unsafeToFuture()
        .map(Right(_))
        .toFutureEither
    }
  }
}
