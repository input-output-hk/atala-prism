package io.iohk.atala.prism.connector.services

import cats.implicits.catsSyntaxEitherId
import io.iohk.atala.prism.connector.errors._
import io.iohk.atala.prism.connector.model._
import io.iohk.atala.prism.connector.repositories.ConnectionsRepository
import io.iohk.atala.prism.kotlin.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.kotlin.identity.{PrismDid => DID}
import io.iohk.atala.prism.kotlin.crypto.keys.ECPublicKey
import io.iohk.atala.prism.kotlin.crypto.ECConfig.{INSTANCE => ECConfig}
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.protos.node_api
import io.iohk.atala.prism.protos.node_api.NodeServiceGrpc
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class ConnectionsService(
    connectionsRepository: ConnectionsRepository[IOWithTraceIdContext],
    nodeService: NodeServiceGrpc.NodeService
)(implicit
    ec: ExecutionContext
) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def getConnectionByToken(token: TokenString): FutureEither[ConnectorError, Option[Connection]] = {
    connectionsRepository
      .getConnectionByToken(token)
      .run(TraceId.generateYOLO)
      .map(_.asRight)
      .unsafeToFuture()
      .toFutureEither
  }

  def generateTokens(userId: ParticipantId, tokensCount: Int): FutureEither[ConnectorError, List[TokenString]] = {
    connectionsRepository
      .insertTokens(userId, List.fill(tokensCount)(TokenString.random()))
      .run(TraceId.generateYOLO)
      .map(_.asRight)
      .unsafeToFuture()
      .toFutureEither
  }

  def getTokenInfo(token: TokenString): FutureEither[ConnectorError, ParticipantInfo] =
    connectionsRepository.getTokenInfo(token).run(TraceId.generateYOLO).unsafeToFuture().toFutureEither

  def addConnectionFromToken(
      tokenString: TokenString,
      didOrPublicKey: Either[DID, ECPublicKey]
  ): FutureEither[ConnectorError, ConnectionInfo] =
    connectionsRepository
      .addConnectionFromToken(tokenString, didOrPublicKey)
      .run(TraceId.generateYOLO)
      .unsafeToFuture()
      .toFutureEither

  def revokeConnection(
      participantId: ParticipantId,
      connectionId: ConnectionId
  ): FutureEither[ConnectorError, Unit] =
    connectionsRepository
      .revokeConnection(participantId, connectionId)
      .run(TraceId.generateYOLO)
      .unsafeToFuture()
      .toFutureEither

  def getConnectionsPaginated(
      userId: ParticipantId,
      limit: Int,
      lastSeenConnectionId: Option[ConnectionId]
  ): FutureEither[ConnectorError, Seq[ConnectionInfo]] =
    connectionsRepository
      .getConnectionsPaginated(userId, limit, lastSeenConnectionId)
      .run(TraceId.generateYOLO)
      .unsafeToFuture()
      .toFutureEither

  def getConnectionCommunicationKeys(
      connectionId: ConnectionId,
      userId: ParticipantId
  ): FutureEither[ConnectorError, Seq[(String, ECPublicKey)]] = {
    def getDidCommunicationKeys(did: DID): FutureEither[ConnectorError, Seq[(String, ECPublicKey)]] = {
      val request = node_api.GetDidDocumentRequest(did = did.getValue)
      val result = for {
        response <- nodeService.getDidDocument(request)
        allKeys = response.document.map(_.publicKeys).getOrElse(Seq.empty)
        validKeys = allKeys.filter(key => key.revokedOn.isEmpty)
        // TODO: select communication keys only, once we provision them and make frontend use them
      } yield validKeys.map { key =>
        val keyData = key.keyData.ecKeyData.getOrElse(throw new Exception("Node returned key without keyData"))
        assert(keyData.curve == ECConfig.getCURVE_NAME)
        (key.id, EC.toPublicKeyFromByteCoordinates(keyData.x.toByteArray, keyData.y.toByteArray))
      }

      result.map(Right(_)).recover { case ex => Left(InternalServerError(ex)) }.toFutureEither
    }

    for {
      participantInfo <-
        connectionsRepository
          .getOtherSideInfo(connectionId, userId)
          .map(_.get.asRight)
          .run(TraceId.generateYOLO)
          .unsafeToFuture()
          .toFutureEither
      keys <- (participantInfo.did, participantInfo.publicKey) match {
        case (Some(did), keyOpt) =>
          if (keyOpt.isDefined) {
            logger.warn(s"Both DID and keys found for user ${userId}, using DID keys only")
          }
          getDidCommunicationKeys(did)
        case (None, Some(key)) => Future.successful(Right(Seq(("", key)))).toFutureEither
        case (None, None) => Future.successful(Right(Seq.empty)).toFutureEither
      }
    } yield keys
  }

  def getConnectionsByConnectionTokens(
      connectionTokens: List[TokenString]
  ): FutureEither[ConnectorError, List[ContactConnection]] =
    connectionsRepository
      .getConnectionsByConnectionTokens(connectionTokens)
      .run(TraceId.generateYOLO)
      .map(_.asRight)
      .unsafeToFuture()
      .toFutureEither
}
