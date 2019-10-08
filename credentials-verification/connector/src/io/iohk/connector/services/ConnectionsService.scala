package io.iohk.connector.services

import java.time.Instant

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

  def getConnectionsSince(
      userId: ParticipantId,
      since: Instant,
      limit: Int
  ): FutureEither[Nothing, Seq[ConnectionInfo]] = {
    connectionsRepository.getConnectionsSince(userId, since, limit)
  }
}
