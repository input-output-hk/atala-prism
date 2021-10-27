package io.iohk.atala.prism.connector.services.logs

import cats.effect.MonadThrow
import cats.syntax.apply._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import io.iohk.atala.prism.connector.errors.ConnectorError
import io.iohk.atala.prism.connector.model.{
  Connection,
  ConnectionId,
  ConnectionInfo,
  ContactConnection,
  ParticipantInfo,
  TokenString
}
import io.iohk.atala.prism.connector.repositories.ConnectionsRepository._
import io.iohk.atala.prism.connector.services.ConnectionsService
import io.iohk.atala.prism.connector.services.ConnectionsService.GetConnectionCommunicationKeysError
import io.iohk.atala.prism.crypto.keys.ECPublicKey
import io.iohk.atala.prism.identity.PrismDid
import io.iohk.atala.prism.models.ParticipantId
import tofu.higherKind.Mid
import tofu.logging.ServiceLogging
import tofu.syntax.logging._

private[services] class ConnectionsServiceLogs[
    F[_]: ServiceLogging[*[_], ConnectionsService[F]]: MonadThrow
] extends ConnectionsService[Mid[F, *]] {
  override def getConnectionByToken(
      token: TokenString
  ): Mid[F, Option[Connection]] =
    in =>
      info"getting connection by token $token" *> in
        .flatTap(res =>
          info"getting connection by token - successfully done, connection ${res
            .fold("not found")(_ => "found")}"
        )
        .onError(
          errorCause"encountered an error while getting connection by token" (_)
        )

  override def generateTokens(
      userId: ParticipantId,
      tokensCount: Int
  ): Mid[F, List[TokenString]] =
    in =>
      info"generating tokens $userId, tokens count - $tokensCount" *> in
        .flatTap(res => info"generating tokens - successfully done, generated ${res.size} tokens")
        .onError(errorCause"encountered an error while generating tokens" (_))

  override def getTokenInfo(
      token: TokenString
  ): Mid[F, Either[GetTokenInfoError, ParticipantInfo]] =
    in =>
      info"getting token info $token" *> in
        .flatTap(res =>
          res.fold(
            er => error"encountered an error while getting token info ${er.unify: ConnectorError}",
            pi => info"getting token info - successfully done ${pi.id}"
          )
        )
        .onError(errorCause"encountered an error while getting token info" (_))

  override def addConnectionFromToken(
      tokenString: TokenString,
      didOrPublicKey: Either[PrismDid, ECPublicKey]
  ): Mid[F, Either[AddConnectionFromTokenError, ConnectionInfo]] =
    in =>
      info"adding connection from token $tokenString" *> in
        .flatTap(res =>
          res.fold(
            er => error"encountered an error while adding connection from token ${er.unify: ConnectorError}",
            pi => info"adding connection from token - successfully done ${pi.id}"
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
      info"revoking connection $connectionId" *> in
        .flatTap(res =>
          res.fold(
            er => error"encountered an error while revoking connection ${er.unify: ConnectorError}",
            _ => info"revoking connection - successfully done"
          )
        )
        .onError(errorCause"encountered an error while revoking connection" (_))

  override def getConnectionById(
      participantId: ParticipantId,
      id: ConnectionId
  ): Mid[F, Option[ConnectionInfo]] =
    in =>
      info"getting connection by id $id, participant $participantId" *> in
        .flatTap(res =>
          info"getting connection by id - successfully done, connection ${res
            .fold("not found")(_ => "found")}"
        )
        .onError(
          errorCause"encountered an error while getting connection by id" (_)
        )

  override def getConnectionsPaginated(
      userId: ParticipantId,
      limit: Int,
      lastSeenConnectionId: Option[ConnectionId]
  ): Mid[F, Either[GetConnectionsPaginatedError, List[ConnectionInfo]]] =
    in =>
      info"getting connections paginated $userId" *> in
        .flatTap(res =>
          res.fold(
            er => error"encountered an error while getting connections paginated ${er.unify: ConnectorError}",
            res => info"getting connections paginated - successfully done, got ${res.size} connections"
          )
        )
        .onError(
          errorCause"encountered an error while getting connections paginated" (
            _
          )
        )

  override def getConnectionCommunicationKeys(
      connectionId: ConnectionId,
      userId: ParticipantId
  ): Mid[F, Either[GetConnectionCommunicationKeysError, Seq[
    (String, ECPublicKey)
  ]]] =
    in =>
      info"getting communications keys $connectionId $userId" *> in
        .flatTap(res =>
          res.fold(
            er => error"encountered an error while getting communications keys  ${er.unify: ConnectorError}",
            _ => info"getting communications keys - successfully done"
          )
        )
        .onError(
          errorCause"encountered an error while getting communications keys" (_)
        )

  override def getConnectionsByConnectionTokens(
      connectionTokens: List[TokenString]
  ): Mid[F, List[ContactConnection]] =
    in =>
      info"getting connections by connection tokens, number of tokens - ${connectionTokens.size}" *> in
        .flatTap(res =>
          info"getting connections by connection tokens - successfully done, generated ${res.size} tokens"
        )
        .onError(
          errorCause"encountered an error while getting connections by connection tokens" (
            _
          )
        )
}
