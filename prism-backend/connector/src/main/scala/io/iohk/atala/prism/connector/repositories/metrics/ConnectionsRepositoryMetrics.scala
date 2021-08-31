package io.iohk.atala.prism.connector.repositories.metrics

import cats.effect.BracketThrow
import io.iohk.atala.prism.connector.errors.ConnectorError
import io.iohk.atala.prism.connector.model._
import io.iohk.atala.prism.connector.repositories.ConnectionsRepository
import io.iohk.atala.prism.kotlin.crypto.keys.ECPublicKey
import io.iohk.atala.prism.kotlin.identity.DID
import io.iohk.atala.prism.metrics.TimeMeasureUtil.MeasureOps
import io.iohk.atala.prism.metrics.{TimeMeasureMetric, TimeMeasureUtil}
import io.iohk.atala.prism.models.ParticipantId
import tofu.higherKind.Mid

private[repositories] final class ConnectionsRepositoryMetrics[F[_]: TimeMeasureMetric: BracketThrow]
    extends ConnectionsRepository[Mid[F, *]] {

  private val repoName = "ConnectionsRepository"
  private lazy val insertTokensTimer = TimeMeasureUtil.createDBQueryTimer(repoName, "insertTokens")
  private lazy val getTokenInfoTimer = TimeMeasureUtil.createDBQueryTimer(repoName, "getTokenInfo")
  private lazy val addConnectionFromTokenTimer = TimeMeasureUtil.createDBQueryTimer(repoName, "addConnectionFromToken")
  private lazy val revokeConnectionsTimer = TimeMeasureUtil.createDBQueryTimer(repoName, "revokeConnection")
  private lazy val getConnectionsPaginatedTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "getConnectionsPaginated")
  private lazy val getOtherSideInfoTimer = TimeMeasureUtil.createDBQueryTimer(repoName, "getOtherSideInfo")
  private lazy val getConnectionByTokenTimer = TimeMeasureUtil.createDBQueryTimer(repoName, "getConnectionByToken")
  private lazy val getConnectionsByConnectionTokensTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "getConnectionsByConnectionTokens")

  override def insertTokens(initiator: ParticipantId, tokens: List[TokenString]): Mid[F, List[TokenString]] =
    _.measureOperationTime(insertTokensTimer)

  override def getTokenInfo(token: TokenString): Mid[F, Either[ConnectorError, ParticipantInfo]] =
    _.measureOperationTime(getTokenInfoTimer)

  override def addConnectionFromToken(
      token: TokenString,
      didOrPublicKey: Either[DID, ECPublicKey]
  ): Mid[F, Either[ConnectorError, ConnectionInfo]] =
    _.measureOperationTime(addConnectionFromTokenTimer)

  override def revokeConnection(
      participantId: ParticipantId,
      connectionId: ConnectionId
  ): Mid[F, Either[ConnectorError, Unit]] = _.measureOperationTime(revokeConnectionsTimer)

  override def getConnectionsPaginated(
      participant: ParticipantId,
      limit: Int,
      lastSeenConnectionId: Option[ConnectionId]
  ): Mid[F, Either[ConnectorError, List[ConnectionInfo]]] = _.measureOperationTime(getConnectionsPaginatedTimer)

  override def getOtherSideInfo(id: ConnectionId, participant: ParticipantId): Mid[F, Option[ParticipantInfo]] =
    _.measureOperationTime(getOtherSideInfoTimer)

  override def getConnectionByToken(token: TokenString): Mid[F, Option[Connection]] =
    _.measureOperationTime(getConnectionByTokenTimer)

  override def getConnectionsByConnectionTokens(connectionTokens: List[TokenString]): Mid[F, List[ContactConnection]] =
    _.measureOperationTime(getConnectionsByConnectionTokensTimer)

}
