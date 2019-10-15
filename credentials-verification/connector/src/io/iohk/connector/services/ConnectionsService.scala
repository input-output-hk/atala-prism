package io.iohk.connector.services

import io.iohk.connector.model._
import io.iohk.connector.repositories.ConnectionsRepository
import io.iohk.cvp.utils.FutureEither

class ConnectionsService(connectionsRepository: ConnectionsRepository) {
  def generateToken(userId: ParticipantId): FutureEither[Nothing, TokenString] = {
    connectionsRepository.insertToken(userId, TokenString.random())
  }

  def getTokenInfo(token: TokenString): FutureEither[Unit, ParticipantInfo] = {
    connectionsRepository.getTokenInfo(token)
  }

  def addConnectionFromToken(userId: ParticipantId, tokenString: TokenString): FutureEither[Unit, ConnectionInfo] = {
    connectionsRepository.addConnectionFromToken(tokenString, userId)
  }

  def getConnectionsPaginated(
      userId: ParticipantId,
      limit: Int,
      lastSeenConnectionId: Option[ConnectionId]
  ): FutureEither[Nothing, Seq[ConnectionInfo]] = {
    connectionsRepository.getConnectionsPaginated(userId, limit, lastSeenConnectionId)
  }
}
