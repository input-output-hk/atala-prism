package io.iohk.cvp.cstore.repositories.daos

import java.util.UUID

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import io.circe.Json
import io.iohk.connector.model.{ConnectionId, TokenString}
import io.iohk.cvp.cstore.models.{IndividualConnectionStatus, StoreIndividual, Verifier, VerifierHolder}
import io.iohk.cvp.models.ParticipantId

object VerifierHoldersDAO {

  case class VerifierHolderCreateData(fullName: String, email: Option[String])

  def insert(verifierId: ParticipantId, data: VerifierHolderCreateData): ConnectionIO[StoreIndividual] = {
    val individualId = ParticipantId.random()
    sql"""
         |INSERT INTO verifier_holders (verifier_id, holder_id, holder_data, created_at)
         |VALUES ($verifierId, $individualId, jsonb_build_object('full_name', ${data.fullName}, 'email', ${data.email}), now())
         |RETURNING holder_id, connection_status, connection_token, connection_id, holder_data ->> 'full_name', holder_data ->> 'email', created_at
       """.stripMargin.query[StoreIndividual].unique
  }

  def insert(verifierId: Verifier.Id, data: Json): ConnectionIO[VerifierHolder] = {
    val holderId = VerifierHolder.Id(UUID.randomUUID())
    sql"""
         |INSERT INTO verifier_holders (verifier_id, holder_id, holder_data, created_at)
         |VALUES ($verifierId, $holderId, $data, now())
         |RETURNING holder_id, holder_data, connection_status, connection_token, connection_id, created_at
       """.stripMargin.query[VerifierHolder].unique
  }

  def list(
      verifierId: ParticipantId,
      lastSeen: Option[ParticipantId],
      limit: Int
  ): ConnectionIO[Seq[StoreIndividual]] = {
    lastSeen match {
      case Some(lastSeen) =>
        // max in CTE select is to force aggregation there is going to be at most one row
        // if it exists we want its value, otherwise default one, hence COALESCE
        sql"""
             |WITH CTE AS (
             |  SELECT COALESCE(max(created_at), to_timestamp(0)) AS last_created_at
             |  FROM verifier_holders
             |  WHERE verifier_id = ${verifierId} AND holder_id = $lastSeen
             |)
             |
             |SELECT holder_id, connection_status, connection_token, connection_id, holder_data ->> 'full_name', holder_data ->> 'email', created_at
             |FROM CTE CROSS JOIN verifier_holders
             |WHERE verifier_id = $verifierId AND (created_at, holder_id) > (last_created_at, $lastSeen)
             |ORDER BY (created_at, holder_id) ASC
             |LIMIT $limit
       """.stripMargin.query[StoreIndividual].to[Seq]
      case None =>
        sql"""
             |SELECT holder_id, connection_status, connection_token, connection_id, holder_data ->> 'full_name', holder_data ->> 'email', created_at
             |FROM verifier_holders
             |WHERE verifier_id = $verifierId
             |ORDER BY (created_at, holder_id) ASC
             |LIMIT $limit
           """.stripMargin.query[StoreIndividual].to[Seq]

    }
  }

  def setConnectionToken(userId: ParticipantId, individualId: ParticipantId, token: TokenString): ConnectionIO[Unit] = {
    sql"""
         |UPDATE verifier_holders
         |SET connection_token = $token, connection_status = ${IndividualConnectionStatus.Invited: IndividualConnectionStatus}
         |WHERE verifier_id = $userId AND holder_id = $individualId
       """.stripMargin.update.run.map(_ => ())
  }

  def addConnection(connectionToken: TokenString, connectionId: ConnectionId): ConnectionIO[Unit] = {
    sql"""
         |UPDATE verifier_holders
         |SET connection_id = $connectionId, connection_status = ${IndividualConnectionStatus.Connected: IndividualConnectionStatus}
         |WHERE connection_token = $connectionToken
       """.stripMargin.update.run.map(_ => ())

  }

}
