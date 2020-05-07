package io.iohk.cvp.cstore.repositories.daos

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import io.iohk.connector.model.{ConnectionId, TokenString}
import io.iohk.cvp.cstore.models.{IndividualConnectionStatus, StoreIndividual}
import io.iohk.cvp.models.ParticipantId

object IndividualsDAO {

  case class StoreIndividualCreateData(fullName: String, email: Option[String])

  def insert(userId: ParticipantId, data: StoreIndividualCreateData): ConnectionIO[StoreIndividual] = {
    val individualId = ParticipantId.random()
    sql"""
         |INSERT INTO verifier_holders (user_id, individual_id, full_name, email, created_at)
         |VALUES ($userId, $individualId, ${data.fullName}, ${data.email}, now())
         |RETURNING individual_id, status, connection_token, connection_id, full_name, email, created_at
       """.stripMargin.query[StoreIndividual].unique
  }

  def list(userId: ParticipantId, lastSeen: Option[ParticipantId], limit: Int): ConnectionIO[Seq[StoreIndividual]] = {
    lastSeen match {
      case Some(lastSeen) =>
        // max in CTE select is to force aggregation there is going to be at most one row
        // if it exists we want its value, otherwise default one, hence COALESCE
        sql"""
             |WITH CTE AS (
             |  SELECT COALESCE(max(created_at), to_timestamp(0)) AS last_created_at
             |  FROM verifier_holders
             |  WHERE user_id = ${userId} AND individual_id = $lastSeen
             |)
             |
             |SELECT individual_id, status, connection_token, connection_id, full_name, email, created_at
             |FROM CTE CROSS JOIN verifier_holders
             |WHERE user_id = $userId AND (created_at, individual_id) > (last_created_at, $lastSeen)
             |ORDER BY (created_at, individual_id) ASC
             |LIMIT $limit
       """.stripMargin.query[StoreIndividual].to[Seq]
      case None =>
        sql"""
             |SELECT individual_id, status, connection_token, connection_id, full_name, email, created_at
             |FROM verifier_holders
             |WHERE user_id = $userId
             |ORDER BY (created_at, individual_id) ASC
             |LIMIT $limit
           """.stripMargin.query[StoreIndividual].to[Seq]

    }
  }

  def setConnectionToken(userId: ParticipantId, individualId: ParticipantId, token: TokenString): ConnectionIO[Unit] = {
    sql"""
         |UPDATE verifier_holders
         |SET connection_token = $token, status = ${IndividualConnectionStatus.Invited: IndividualConnectionStatus}
         |WHERE user_id = $userId AND individual_id = $individualId
       """.stripMargin.update.run.map(_ => ())
  }

  def addConnection(connectionToken: TokenString, connectionId: ConnectionId): ConnectionIO[Unit] = {
    sql"""
         |UPDATE verifier_holders
         |SET connection_id = $connectionId, status = ${IndividualConnectionStatus.Connected: IndividualConnectionStatus}
         |WHERE connection_token = $connectionToken
       """.stripMargin.update.run.map(_ => ())

  }

}
