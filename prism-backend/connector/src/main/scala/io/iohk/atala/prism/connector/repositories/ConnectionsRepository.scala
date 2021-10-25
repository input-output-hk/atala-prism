package io.iohk.atala.prism.connector.repositories

import cats.{Comonad, Functor}
import cats.data.{EitherT, OptionT}
import cats.effect.BracketThrow
import cats.implicits.{catsSyntaxApplicativeId, catsSyntaxEitherId, catsSyntaxOptionId}
import cats.syntax.either._
import cats.syntax.comonad._
import cats.syntax.functor._
import derevo.derive
import derevo.tagless.applyK
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.connector.errors.ConnectionsError._
import io.iohk.atala.prism.crypto.keys.ECPublicKey
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import io.iohk.atala.prism.connector.errors.{UnknownValueError, _}
import io.iohk.atala.prism.connector.model._
import io.iohk.atala.prism.connector.repositories.daos.{
  ConnectionTokensDAO,
  ConnectionsDAO,
  MessagesDAO,
  ParticipantsDAO
}
import io.iohk.atala.prism.connector.repositories.logs.ConnectionsRepositoryLogs
import io.iohk.atala.prism.connector.repositories.metrics.ConnectionsRepositoryMetrics
import io.iohk.atala.prism.errors.LoggingContext
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.metrics.TimeMeasureMetric
import org.slf4j.{Logger, LoggerFactory}
import shapeless.{:+:, CNil}
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}
import tofu.syntax.monoid.TofuSemigroupOps

@derive(applyK)
trait ConnectionsRepository[F[_]] {

  import io.iohk.atala.prism.connector.repositories.ConnectionsRepository._

  def insertTokens(
      initiator: ParticipantId,
      tokens: List[TokenString]
  ): F[List[TokenString]]

  def getTokenInfo(
      token: TokenString
  ): F[Either[GetTokenInfoError, ParticipantInfo]]

  def addConnectionFromToken(
      token: TokenString,
      didOrPublicKey: Either[DID, ECPublicKey]
  ): F[Either[AddConnectionFromTokenError, ConnectionInfo]]

  def revokeConnection(
      participantId: ParticipantId,
      connectionId: ConnectionId
  ): F[Either[RevokeConnectionError, Unit]]

  def getConnection(
      participant: ParticipantId,
      id: ConnectionId
  ): F[Option[ConnectionInfo]]

  def getConnectionsPaginated(
      participant: ParticipantId,
      limit: Int,
      lastSeenConnectionId: Option[ConnectionId]
  ): F[Either[GetConnectionsPaginatedError, List[ConnectionInfo]]]

  def getOtherSideInfo(
      id: ConnectionId,
      participant: ParticipantId
  ): F[Option[ParticipantInfo]]

  def getConnectionByToken(token: TokenString): F[Option[Connection]]

  def getConnectionsByConnectionTokens(
      connectionTokens: List[TokenString]
  ): F[List[ContactConnection]]
}

object ConnectionsRepository {

  type GetTokenInfoError = UnknownValueError :+: CNil

  type AddConnectionFromTokenError =
    DidConnectionExist :+:
      PkConnectionExist :+:
      UnknownValueError :+:
      CNil

  type GetConnectionsPaginatedError = InvalidArgumentError :+: CNil

  type RevokeConnectionError =
    UnknownValueError :+: InternalConnectorError :+: CNil

  def apply[F[_]: TimeMeasureMetric: BracketThrow, R[_]: Functor](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): R[ConnectionsRepository[F]] =
    for {
      serviceLogs <- logs.service[ConnectionsRepository[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, ConnectionsRepository[F]] =
        serviceLogs
      val metrics: ConnectionsRepository[Mid[F, *]] =
        new ConnectionsRepositoryMetrics[F]
      val logs: ConnectionsRepository[Mid[F, *]] =
        new ConnectionsRepositoryLogs[F]
      val mid = metrics |+| logs
      mid attach new ConnectionsRepositoryPostgresImpl[F](transactor)
    }

  def unsafe[F[_]: TimeMeasureMetric: BracketThrow, R[_]: Comonad](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): ConnectionsRepository[F] = ConnectionsRepository(transactor, logs).extract
}

private final class ConnectionsRepositoryPostgresImpl[F[_]: BracketThrow](
    xa: Transactor[F]
) extends ConnectionsRepository[F]
    with ConnectorErrorSupportNew {

  import io.iohk.atala.prism.connector.repositories.ConnectionsRepository._

  val logger: Logger = LoggerFactory.getLogger(getClass)

  override def insertTokens(
      initiator: ParticipantId,
      tokens: List[TokenString]
  ): F[List[TokenString]] = {
    ConnectionTokensDAO
      .insert(initiator, tokens)
      .logSQLErrors("inserting tokens", logger)
      .transact(xa)
      .as(tokens)
  }

  override def getTokenInfo(
      token: TokenString
  ): F[Either[GetTokenInfoError, ParticipantInfo]] = {
    implicit val loggingContext = LoggingContext("token" -> token)

    ParticipantsDAO
      .findBy(token)
      .toRight[GetTokenInfoError](
        co(UnknownValueError("token", token.token).logWarnNew)
      )
      .value
      .logSQLErrors("getting token info", logger)
      .transact(xa)
  }

  override def addConnectionFromToken(
      token: TokenString,
      didOrPublicKey: Either[DID, ECPublicKey]
  ): F[Either[AddConnectionFromTokenError, ConnectionInfo]] = {

    val maybeDid = didOrPublicKey.left.toOption
    val maybePublicKey = didOrPublicKey.toOption

    implicit val loggingContext: LoggingContext =
      LoggingContext(
        "token" -> token,
        "did" -> maybeDid,
        "publicKey" -> maybePublicKey
      )

    // if exist Left(ConnectorError) else Right(Unit)
    val checkIfAlreadyExist: EitherT[doobie.ConnectionIO, AddConnectionFromTokenError, Unit] =
      EitherT(
        didOrPublicKey
          .fold(
            did =>
              ParticipantsDAO
                .findByDID(did)
                .value
                .map(
                  _.fold[Option[AddConnectionFromTokenError]](Option.empty)(_ =>
                    co[AddConnectionFromTokenError](
                      DidConnectionExist(did)
                    ).some
                  )
                ),
            pk =>
              ParticipantsDAO
                .findByPublicKey(pk)
                .value
                .map(
                  _.fold[Option[AddConnectionFromTokenError]](Option.empty)(_ =>
                    co[AddConnectionFromTokenError](PkConnectionExist(pk)).some
                  )
                )
          )
          .map(_.toLeft(()))
      )

    val query: EitherT[
      doobie.ConnectionIO,
      AddConnectionFromTokenError,
      ConnectionInfo
    ] = for {
      initiator <-
        ParticipantsDAO
          .findByAvailableToken(token)
          .toRight(
            co[AddConnectionFromTokenError](
              UnknownValueError("token", token.token).logWarnNew
            )
          )

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

      _ <- EitherT.right[AddConnectionFromTokenError] {
        ParticipantsDAO.insert(acceptorInfo)
      }

      ciia <- EitherT.right[AddConnectionFromTokenError](
        ConnectionsDAO.insert(
          initiator = initiator.id,
          acceptor = acceptorInfo.id,
          token = token,
          connectionStatus = ConnectionStatus.ConnectionAccepted
        )
      )
      (connectionId, instantiatedAt) = ciia

      _ <- EitherT.right[AddConnectionFromTokenError](
        ConnectionTokensDAO.markAsUsed(token)
      )
    } yield ConnectionInfo(
      connectionId,
      instantiatedAt,
      initiator,
      token,
      ConnectionStatus.ConnectionAccepted
    )

    query.value
      .logSQLErrors("adding connection from token", logger)
      .transact(xa)
  }

  override def revokeConnection(
      participantId: ParticipantId,
      connectionId: ConnectionId
  ): F[Either[RevokeConnectionError, Unit]] = {
    // verify the connection belongs to the participant, and its connected
    def verifyOwnership: EitherT[doobie.ConnectionIO, RevokeConnectionError, Unit] =
      for {
        connectionMaybe <- EitherT(
          ConnectionsDAO
            .getRawConnection(connectionId)
            .map(_.asRight[RevokeConnectionError])
        )
        resultE = connectionMaybe match {
          // The connection can't be revoked when the participant is not involved in the connection,
          // or the connection is not established
          case Some(connection)
              if connection.contains(
                participantId
              ) && connection.status == ConnectionStatus.ConnectionAccepted =>
            ().asRight[RevokeConnectionError]
          case _ =>
            co[RevokeConnectionError](
              UnknownValueError("connectionId", connectionId.uuid.toString)
            ).asLeft[Unit]
        }
        _ <- EitherT(doobie.free.connection.pure(resultE))
      } yield ()

    val query: EitherT[doobie.ConnectionIO, RevokeConnectionError, Unit] = for {
      _ <- verifyOwnership
      affectedRows <- EitherT {
        ConnectionsDAO
          .revoke(connectionId)
          .map(_.asRight[RevokeConnectionError])
      }
      _ <- EitherT.cond[doobie.ConnectionIO](
        affectedRows == 1,
        (),
        co[RevokeConnectionError](
          InternalConnectorError(
            new RuntimeException(
              "Unable to revoke the connection, please try again later"
            )
          )
        )
      )
      // TODO: Remove once messages are being removed after they are read
      _ <- EitherT {
        MessagesDAO
          .deleteConnectionMessages(connectionId)
          .map(_.asRight[RevokeConnectionError])
      }
    } yield ()

    query.value
      .logSQLErrors(s"revoke connection, connection id - $connectionId", logger)
      .transact(xa)
  }

  override def getConnection(
      participant: ParticipantId,
      id: ConnectionId
  ): F[Option[ConnectionInfo]] = {
    // finds the connection making sure it is accessible by the participant
    def safeQuery =
      ConnectionsDAO
        .getRawConnection(id)
        .map { maybe =>
          maybe.filter(_.contains(participant))
        }

    val query = for {
      rawConnection <- OptionT(safeQuery)

      otherParticipantId = {
        if (rawConnection.initiator == participant) rawConnection.acceptor
        else rawConnection.initiator
      }

      otherParticipant <-
        ParticipantsDAO
          .findBy(otherParticipantId)
    } yield ConnectionInfo(
      rawConnection.id,
      rawConnection.instantiatedAt,
      otherParticipant,
      rawConnection.token,
      rawConnection.status
    )

    query.value
      .logSQLErrors(
        s"getConnection, id - $id, participant - $participant",
        logger
      )
      .transact(xa)
  }

  override def getConnectionsPaginated(
      participant: ParticipantId,
      limit: Int,
      lastSeenConnectionId: Option[ConnectionId]
  ): F[Either[GetConnectionsPaginatedError, List[ConnectionInfo]]] = {
    implicit val loggingContext = LoggingContext("participant" -> participant)

    if (limit <= 0)
      InvalidArgumentError("limit", "positive value", limit.toString).logWarnNew
        .asLeft[List[ConnectionInfo]]
        .leftMap[GetConnectionsPaginatedError](co(_))
        .pure[F]
    else
      ConnectionsDAO
        .getConnectionsPaginated(participant, limit, lastSeenConnectionId)
        .logSQLErrors("getting connection paginated", logger)
        .transact(xa)
        .map(_.asRight)
  }

  override def getOtherSideInfo(
      id: ConnectionId,
      userId: ParticipantId
  ): F[Option[ParticipantInfo]] = {
    val query = for {
      participantId <- OptionT(ConnectionsDAO.getOtherSide(id, userId))
      participantInfo <- ParticipantsDAO.findBy(participantId)
    } yield participantInfo

    query.value
      .logSQLErrors(s"getting other side info, connection id - $id", logger)
      .transact(xa)
  }

  override def getConnectionByToken(token: TokenString): F[Option[Connection]] =
    ConnectionsDAO
      .getConnectionByToken(token)
      .logSQLErrors("getting connection by token", logger)
      .transact(xa)

  override def getConnectionsByConnectionTokens(
      connectionTokens: List[TokenString]
  ): F[List[ContactConnection]] =
    ConnectionsDAO
      .getConnectionsByConnectionTokens(connectionTokens)
      .logSQLErrors("getting by connection tokens", logger)
      .transact(xa)
}
