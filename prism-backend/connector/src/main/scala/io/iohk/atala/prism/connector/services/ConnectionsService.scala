package io.iohk.atala.prism.connector.services

import io.iohk.atala.prism.connector.errors._
import io.iohk.atala.prism.connector.model._
import io.iohk.atala.prism.connector.repositories.ConnectionsRepository
import io.iohk.atala.prism.crypto.{EC, ECConfig, ECPublicKey}
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.protos.node_api
import io.iohk.atala.prism.protos.node_api.NodeServiceGrpc
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class ConnectionsService(connectionsRepository: ConnectionsRepository, nodeService: NodeServiceGrpc.NodeService)(
    implicit ec: ExecutionContext
) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def getConnectionByToken(token: TokenString): FutureEither[ConnectorError, Option[Connection]] = {
    connectionsRepository.getConnectionByToken(token)
  }

  def generateToken(userId: ParticipantId): FutureEither[ConnectorError, TokenString] = {
    connectionsRepository.insertToken(userId, TokenString.random())
  }

  def getTokenInfo(token: TokenString): FutureEither[ConnectorError, ParticipantInfo] = {
    connectionsRepository.getTokenInfo(token)
  }

  def addConnectionFromToken(
      tokenString: TokenString,
      publicKey: ECPublicKey
  ): FutureEither[ConnectorError, (ParticipantId, ConnectionInfo)] = {
    connectionsRepository.addConnectionFromToken(tokenString, publicKey)
  }

  def getConnectionsPaginated(
      userId: ParticipantId,
      limit: Int,
      lastSeenConnectionId: Option[ConnectionId]
  ): FutureEither[ConnectorError, Seq[ConnectionInfo]] = {
    connectionsRepository.getConnectionsPaginated(userId, limit, lastSeenConnectionId)
  }

  def getConnectionCommunicationKeys(
      connectionId: ConnectionId,
      userId: ParticipantId
  ): FutureEither[ConnectorError, Seq[(String, ECPublicKey)]] = {
    def getDidCommunicationKeys(did: DID): FutureEither[ConnectorError, Seq[(String, ECPublicKey)]] = {
      val request = node_api.GetDidDocumentRequest(did = did.value)
      val result = for {
        response <- nodeService.getDidDocument(request)
        allKeys = response.document.map(_.publicKeys).getOrElse(Seq.empty)
        validKeys = allKeys.filter(key => key.revokedOn.isEmpty)
        // TODO: select communication keys only, once we provision them and make frontend use them
      } yield validKeys.map { key =>
        val keyData = key.keyData.ecKeyData.getOrElse(throw new Exception("Node returned key without keyData"))
        assert(keyData.curve == ECConfig.CURVE_NAME)
        (key.id, EC.toPublicKey(keyData.x.toByteArray, keyData.y.toByteArray))
      }

      result.map(Right(_)).recover { case ex => Left(InternalServerError(ex)) }.toFutureEither
    }

    for {
      participantInfo <- connectionsRepository.getOtherSideInfo(connectionId, userId).map(_.get)
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

  def getAcceptorConnections(
      acceptorIds: List[ParticipantId]
  ): FutureEither[ConnectorError, List[ContactConnection]] = {
    connectionsRepository.getAcceptorConnections(acceptorIds)
  }
}
