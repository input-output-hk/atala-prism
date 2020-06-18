package io.iohk.connector.repositories

import cats.data.EitherT
import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.connector.errors._
import io.iohk.connector.model._
import io.iohk.connector.repositories.daos.{ConnectionTokensDAO, ConnectionsDAO, ParticipantsDAO}
import io.iohk.cvp.cmanager.models
import io.iohk.cvp.cmanager.repositories.daos.IssuerSubjectsDAO
import io.iohk.cvp.crypto.ECKeys.EncodedPublicKey
import io.iohk.cvp.cstore.repositories.daos.VerifierHoldersDAO
import io.iohk.cvp.models.ParticipantId
import io.iohk.cvp.utils.FutureEither
import io.iohk.cvp.utils.FutureEither._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext

trait ConnectionsRepository {

  def insertToken(initiator: ParticipantId, token: TokenString): FutureEither[Nothing, TokenString]

  def getTokenInfo(token: TokenString): FutureEither[ConnectorError, ParticipantInfo]

  def addConnectionFromToken(
      token: TokenString,
      publicKey: EncodedPublicKey
  ): FutureEither[ConnectorError, (ParticipantId, ConnectionInfo)]

  def getConnectionsPaginated(
      participant: ParticipantId,
      limit: Int,
      lastSeenConnectionId: Option[ConnectionId]
  ): FutureEither[ConnectorError, Seq[ConnectionInfo]]

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
        publicKey: EncodedPublicKey
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
          IssuerSubjectsDAO.update(
            models.Issuer.Id(initiator.id.uuid),
            IssuerSubjectsDAO.UpdateSubjectRequest.ConnectionAccepted(token, connectionId)
          )
        }

        // corresponding hack to add connectionId to the individual in cstore, TODO: ditto
        _ <- EitherT.right[ConnectorError] {
          VerifierHoldersDAO.addConnection(token, connectionId)
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
