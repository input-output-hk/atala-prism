package io.iohk.atala.prism.connector.repositories

import cats.{Comonad, Functor}
import cats.data.{EitherT, OptionT}
import cats.effect.BracketThrow
import cats.implicits.{catsSyntaxApplicativeId, catsSyntaxEitherId, catsSyntaxOptionId}
import cats.syntax.comonad._
import cats.syntax.functor._
import derevo.derive
import derevo.tagless.applyK
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.kotlin.crypto.keys.ECPublicKey
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import io.iohk.atala.prism.connector.errors._
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
import io.iohk.atala.prism.kotlin.identity.{PrismDid => DID}
import io.iohk.atala.prism.metrics.TimeMeasureMetric
import org.slf4j.{Logger, LoggerFactory}
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}
import tofu.syntax.monoid.TofuSemigroupOps

@derive(applyK)
trait ConnectionsRepository[F[_]] {

  def insertTokens(initiator: ParticipantId, tokens: List[TokenString]): F[List[TokenString]]

  def getTokenInfo(token: TokenString): F[Either[ConnectorError, ParticipantInfo]]

  def addConnectionFromToken(
      token: TokenString,
      didOrPublicKey: Either[DID, ECPublicKey]
  ): F[Either[ConnectorError, ConnectionInfo]]

  def revokeConnection(participantId: ParticipantId, connectionId: ConnectionId): F[Either[ConnectorError, Unit]]

  def getConnectionsPaginated(
      participant: ParticipantId,
      limit: Int,
      lastSeenConnectionId: Option[ConnectionId]
  ): F[Either[ConnectorError, List[ConnectionInfo]]]

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
  def apply[F[_]: TimeMeasureMetric: BracketThrow, R[_]: Functor](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): R[ConnectionsRepository[F]] =
    for {
      serviceLogs <- logs.service[ConnectionsRepository[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, ConnectionsRepository[F]] = serviceLogs
      val metrics: ConnectionsRepository[Mid[F, *]] = new ConnectionsRepositoryMetrics[F]
      val logs: ConnectionsRepository[Mid[F, *]] = new ConnectionsRepositoryLogs[F]
      val mid = metrics |+| logs
      mid attach new ConnectionsRepositoryPostgresImpl[F](transactor)
    }

  def unsafe[F[_]: TimeMeasureMetric: BracketThrow, R[_]: Comonad](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): ConnectionsRepository[F] = ConnectionsRepository(transactor, logs).extract

}

private final class ConnectionsRepositoryPostgresImpl[F[_]: BracketThrow](xa: Transactor[F])
    extends ConnectionsRepository[F]
    with ConnectorErrorSupport {
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

  override def getTokenInfo(token: TokenString): F[Either[ConnectorError, ParticipantInfo]] = {
    implicit val loggingContext = LoggingContext("token" -> token)

    ParticipantsDAO
      .findBy(token)
      .toRight(UnknownValueError("token", token.token).logWarn)
      .value
      .logSQLErrors("getting token info", logger)
      .transact(xa)
  }

  override def addConnectionFromToken(
      token: TokenString,
      didOrPublicKey: Either[DID, ECPublicKey]
  ): F[Either[ConnectorError, ConnectionInfo]] = {

    val maybeDid = didOrPublicKey.left.toOption
    val maybePublicKey = didOrPublicKey.toOption

    implicit val loggingContext: LoggingContext =
      LoggingContext("token" -> token, "did" -> maybeDid, "publicKey" -> maybePublicKey)

    // if exist Left(ConnectorError) else Right(Unit)
    val checkIfAlreadyExist: EitherT[doobie.ConnectionIO, ConnectorError, Unit] = EitherT(
      didOrPublicKey
        .fold(
          did =>
            ParticipantsDAO
              .findByDID(did)
              .value
              .map(_.fold[Option[ConnectorError]](Option.empty)(_ => DidConnectionExist(did).some)),
          pk =>
            ParticipantsDAO
              .findByPublicKey(pk)
              .value
              .map(_.fold[Option[ConnectorError]](Option.empty)(_ => PkConnectionExist(pk).some))
        )
        .map(_.toRight(()).swap)
    )

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

      _ <- EitherT.right[ConnectorError](ConnectionTokensDAO.markAsUsed(token))
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
  ): F[Either[ConnectorError, Unit]] = {
    // verify the connection belongs to the participant, and its connected
    def verifyOwnership =
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
      // TODO: Remove once messages are being removed after they are read
      _ <- EitherT {
        MessagesDAO.deleteConnectionMessages(connectionId).map(_.asRight[ConnectorError])
      }
    } yield ()

    query.value
      .logSQLErrors(s"revoke connection, connection id - $connectionId", logger)
      .transact(xa)
  }

  override def getConnectionsPaginated(
      participant: ParticipantId,
      limit: Int,
      lastSeenConnectionId: Option[ConnectionId]
  ): F[Either[ConnectorError, List[ConnectionInfo]]] = {
    implicit val loggingContext = LoggingContext("participant" -> participant)

    if (limit <= 0)
      InvalidArgumentError("limit", "positive value", limit.toString).logWarn.asLeft[List[ConnectionInfo]].pure[F]
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
