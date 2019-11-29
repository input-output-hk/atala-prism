package io.iohk.connector.repositories

import cats.data.EitherT
import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.connector.errors._
import io.iohk.connector.model._
import io.iohk.connector.repositories.daos.{ConnectionTokensDAO, ConnectionsDAO, ParticipantsDAO}
import io.iohk.cvp.models.ParticipantId
import io.iohk.cvp.utils.FutureEither
import io.iohk.cvp.utils.FutureEither._
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext

class ConnectionsRepository(
    xa: Transactor[IO]
)(implicit ec: ExecutionContext)
    extends ErrorSupport {

  val logger = LoggerFactory.getLogger(getClass)

  def insertToken(initiator: ParticipantId, token: TokenString): FutureEither[Nothing, TokenString] = {
    implicit val loggingContext = LoggingContext("token" -> token, "initiator" -> initiator)

    ConnectionTokensDAO
      .insert(initiator, token)
      .transact(xa)
      .unsafeToFuture()
      .map(_ => Right(token))
      .toFutureEither
  }

  def getTokenInfo(token: TokenString): FutureEither[ConnectorError, ParticipantInfo] = {
    implicit val loggingContext = LoggingContext("token" -> token)

    ParticipantsDAO
      .findBy(token)
      .toRight(UnknownValueError("token", token.token).logWarn)
      .transact(xa)
      .value
      .unsafeToFuture()
      .toFutureEither
  }

  def addConnectionFromToken(
      token: TokenString,
      publicKey: ECPublicKey
  ): FutureEither[ConnectorError, (ParticipantId, ConnectionInfo)] = {

    implicit val loggingContext = LoggingContext("token" -> token)

    val query = for {
      initiator <- ParticipantsDAO
        .findByAvailableToken(token)
        .toRight(UnknownValueError("token", token.token).logWarn)

      // Create a holder, which has no name nor did, instead it has a public key
      acceptorInfo = ParticipantInfo(ParticipantId.random(), ParticipantType.Holder, "", None)
      _ <- EitherT.right[ConnectorError] {
        ParticipantsDAO.insert(acceptorInfo)
      }
      _ <- EitherT.right[ConnectorError] {
        ParticipantsDAO.insertPublicKey(acceptorInfo.id, publicKey)
      }

      ciia <- EitherT.right[ConnectorError](
        ConnectionsDAO.insert(initiator = initiator.id, acceptor = acceptorInfo.id, token = token)
      )
      (connectionId, instantiatedAt) = ciia

      _ <- EitherT.right[ConnectorError](ConnectionTokensDAO.markAsUsed(token))
    } yield acceptorInfo.id -> ConnectionInfo(connectionId, instantiatedAt, initiator, token)

    query
      .transact(xa)
      .value
      .unsafeToFuture()
      .toFutureEither
  }

  def getConnectionsPaginated(
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
}
