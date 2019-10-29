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
      userId: ParticipantId,
      tokenString: TokenString
  ): FutureEither[ConnectorError, ConnectionInfo] = {
    connectionsRepository.addConnectionFromToken(tokenString, userId)
  }

  def getConnectionsPaginated(
      userId: ParticipantId,
      limit: Int,
      lastSeenConnectionId: Option[ConnectionId]
  ): FutureEither[ConnectorError, Seq[ConnectionInfo]] = {
    connectionsRepository.getConnectionsPaginated(userId, limit, lastSeenConnectionId)
  }
}
