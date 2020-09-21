package io.iohk.atala.prism.connector.repositories

import cats.data.{EitherT, OptionT}
import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.crypto.ECPublicKey
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither._
import io.iohk.atala.prism.connector.errors._
import io.iohk.atala.prism.connector.model._
import io.iohk.atala.prism.connector.repositories.daos.{ConnectionTokensDAO, ConnectionsDAO, ParticipantsDAO}
import io.iohk.atala.prism.console.models.Institution
import io.iohk.atala.prism.console.repositories.daos.ContactsDAO
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext

trait ConnectionsRepository {

  def insertToken(initiator: ParticipantId, token: TokenString): FutureEither[Nothing, TokenString]

  def getTokenInfo(token: TokenString): FutureEither[ConnectorError, ParticipantInfo]

  def addConnectionFromToken(
      token: TokenString,
      publicKey: ECPublicKey
  ): FutureEither[ConnectorError, (ParticipantId, ConnectionInfo)]

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
}

object ConnectionsRepository {
  class PostgresImpl(xa: Transactor[IO])(implicit ec: ExecutionContext)
      extends ConnectionsRepository
      with ErrorSupport {
    val logger: Logger = LoggerFactory.getLogger(getClass)

    override def insertToken(initiator: ParticipantId, token: TokenString): FutureEither[Nothing, TokenString] = {
      ConnectionTokensDAO
        .insert(initiator, token)
        .transact(xa)
        .unsafeToFuture()
        .map(_ => Right(token))
        .toFutureEither
    }

    override def getTokenInfo(token: TokenString): FutureEither[ConnectorError, ParticipantInfo] = {
      implicit val loggingContext = LoggingContext("token" -> token)

      ParticipantsDAO
        .findBy(token)
        .toRight(UnknownValueError("token", token.token).logWarn)
        .transact(xa)
        .value
        .unsafeToFuture()
        .toFutureEither
    }

    override def addConnectionFromToken(
        token: TokenString,
        publicKey: ECPublicKey
    ): FutureEither[ConnectorError, (ParticipantId, ConnectionInfo)] = {

      implicit val loggingContext = LoggingContext("token" -> token)

      val query = for {
        initiator <-
          ParticipantsDAO
            .findByAvailableToken(token)
            .toRight(UnknownValueError("token", token.token).logWarn)

        // Create a holder, which has no name nor did, instead it has a public key
        acceptorInfo = ParticipantInfo(ParticipantId.random(), ParticipantType.Holder, Some(publicKey), "", None, None)
        _ <- EitherT.right[ConnectorError] {
          ParticipantsDAO.insert(acceptorInfo)
        }

        ciia <- EitherT.right[ConnectorError](
          ConnectionsDAO.insert(initiator = initiator.id, acceptor = acceptorInfo.id, token = token)
        )
        (connectionId, instantiatedAt) = ciia

        // hack to add the connectionId to the student (if any), TODO: this should be moved to another layer
        _ <- EitherT.right[ConnectorError] {
          ContactsDAO.setConnectionAsAccepted(
            Institution.Id(initiator.id.uuid),
            token,
            connectionId
          )
        }

        _ <- EitherT.right[ConnectorError](ConnectionTokensDAO.markAsUsed(token))
      } yield acceptorInfo.id -> ConnectionInfo(connectionId, instantiatedAt, initiator, token)

      query
        .transact(xa)
        .value
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
        .transact(xa)
        .unsafeToFuture()
        .map(Right(_))
        .toFutureEither
    }

    override def getConnectionByToken(token: TokenString): FutureEither[ConnectorError, Option[Connection]] = {
      ConnectionsDAO
        .getConnectionByToken(token)
        .transact(xa)
        .unsafeToFuture()
        .map(Right(_))
        .toFutureEither
    }
  }
}
