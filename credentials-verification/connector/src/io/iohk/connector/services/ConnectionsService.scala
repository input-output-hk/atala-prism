package io.iohk.connector.services

import io.iohk.connector.errors._
import io.iohk.connector.model._
import io.iohk.connector.repositories.ConnectionsRepository
import io.iohk.cvp.utils.FutureEither

class ConnectionsService(connectionsRepository: ConnectionsRepository) {
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
}
