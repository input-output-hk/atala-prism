package io.iohk.atala.prism.connector.repositories.logs

import cats.syntax.apply._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import io.iohk.atala.prism.connector.errors.ConnectorError
import io.iohk.atala.prism.connector.model._
import io.iohk.atala.prism.connector.repositories.ConnectionsRepository
import io.iohk.atala.prism.connector.repositories.ConnectionsRepository._
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.crypto.keys.ECPublicKey
import io.iohk.atala.prism.models.ParticipantId
import tofu.higherKind.Mid
import tofu.logging.ServiceLogging
import tofu.syntax.logging._
import cats.MonadThrow

private[repositories] final class ConnectionsRepositoryLogs[F[
    _
]: ServiceLogging[
  *[_],
  ConnectionsRepository[F]
]: MonadThrow]
    extends ConnectionsRepository[Mid[F, *]] {
  override def insertTokens(
      initiator: ParticipantId,
      tokens: List[TokenString]
  ): Mid[F, List[TokenString]] =
    in =>
      info"inserting tokens $initiator ${tokens.size} entities" *> in
        .flatTap(result => info"inserting tokens - successfully done, inserted ${result.size} tokens")
        .onError(errorCause"encountered an error while inserting tokens" (_))

  override def getTokenInfo(
      token: TokenString
  ): Mid[F, Either[GetTokenInfoError, ParticipantInfo]] =
    in =>
      info"getting token info $token" *> in
        .flatTap(
          _.fold(
            er => error"encountered an error while getting token info ${er.unify: ConnectorError}",
            info => info"getting token info - successfully done ${info.id}"
          )
        )
        .onError(errorCause"encountered an error while getting token info" (_))

  override def addConnectionFromToken(
      token: TokenString,
      didOrPublicKey: Either[DID, ECPublicKey]
  ): Mid[F, Either[AddConnectionFromTokenError, ConnectionInfo]] =
    in =>
      info"adding connection from token $token" *> in
        .flatTap(
          _.fold(
            er => error"encountered an error while adding connection from token ${er.unify: ConnectorError}",
            info => info"adding connection from token - successfully done ${info.id}"
          )
        )
        .onError(
          errorCause"encountered an error while adding connection from token" (
            _
          )
        )

  override def revokeConnection(
      participantId: ParticipantId,
      connectionId: ConnectionId
  ): Mid[F, Either[RevokeConnectionError, Unit]] =
    in =>
      info"revoking connection $participantId $connectionId" *> in
        .flatTap(
          _.fold(
            er => error"encountered an error while revoking connection ${er.unify: ConnectorError}",
            _ => info"revoking connection - successfully done"
          )
        )
        .onError(errorCause"encountered an error while revoking connection" (_))

  override def getConnection(
      participant: ParticipantId,
      id: ConnectionId
  ): Mid[F, Option[ConnectionInfo]] =
    in =>
      info"getting connection by id" *> in
        .flatTap(result =>
          info"getting connection by id - successfully done, ${result
            .fold("not found")(_ => "found")}"
        )
        .onError(
          errorCause"encountered an error while getting connection by id" (_)
        )

  override def getConnectionsPaginated(
      participant: ParticipantId,
      limit: Int,
      lastSeenConnectionId: Option[ConnectionId]
  ): Mid[F, Either[GetConnectionsPaginatedError, List[ConnectionInfo]]] =
    in =>
      info"getting connections paginated $participant $lastSeenConnectionId" *> in
        .flatTap(
          _.fold(
            er => error"encountered an error while getting connections paginated ${er.unify: ConnectorError}",
            list => info"getting connections paginated - successfully done, got ${list.size} entities"
          )
        )
        .onError(
          errorCause"encountered an error while getting connections paginated" (
            _
          )
        )

  override def getOtherSideInfo(
      id: ConnectionId,
      participant: ParticipantId
  ): Mid[F, Option[ParticipantInfo]] =
    in =>
      info"getting other side info" *> in
        .flatTap(result =>
          info"getting other side info - successfully done, ${result
            .fold("not found")(_ => "found")}"
        )
        .onError(
          errorCause"encountered an error while getting other side info" (_)
        )

  override def getConnectionByToken(
      token: TokenString
  ): Mid[F, Option[Connection]] =
    in =>
      info"getting connection by token" *> in
        .flatTap(result =>
          info"getting connection by token - successfully done, ${result
            .fold("not found")(_ => "found")}"
        )
        .onError(
          errorCause"encountered an error while getting connection by token" (_)
        )

  override def getConnectionsByConnectionTokens(
      connectionTokens: List[TokenString]
  ): Mid[F, List[ContactConnection]] =
    in =>
      info"getting connections by connection token" *> in
        .flatTap(result =>
          info"getting connections by connection token - successfully done, got ${result.size} entities"
        )
        .onError(
          errorCause"encountered an error while getting connections by connection token" (
            _
          )
        )
}
