package io.iohk.atala.prism.vault.repositories.daos

import java.time.Instant
import java.util.UUID

import doobie.ConnectionIO
import doobie.implicits.toSqlInterpolator
import doobie.implicits.legacy.instant._
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.vault.model.{CreatePayload, Payload}

object PayloadsDAO {
  def createPayload(data: CreatePayload): ConnectionIO[Payload] = {
    val payloadId = Payload.Id(UUID.randomUUID())
    val createdAt = Instant.now()

    sql"""
         |INSERT INTO payloads (payload_id, did, content, created_at)
         |VALUES ($payloadId, ${data.did}, ${data.content}, $createdAt)
         |RETURNING payload_id, did, content, created_at
         |""".stripMargin.query[Payload].unique
  }

  def getByPaginated(
      did: DID,
      lastSeenIdOpt: Option[Payload.Id],
      limit: Int
  ): ConnectionIO[List[Payload]] = {
    val query = lastSeenIdOpt match {
      case Some(lastSeenId) =>
        sql"""WITH CTE AS (
             |  SELECT creation_order AS latest_seen_creation_order
             |  FROM payloads
             |  WHERE payload_id = $lastSeenId
             |)
             |SELECT payload_id, did, content, created_at
             |FROM CTE CROSS JOIN payloads
             |WHERE did = $did AND creation_order > latest_seen_creation_order
             |ORDER BY creation_order ASC
             |LIMIT $limit
             |""".stripMargin
      case None =>
        sql"""SELECT payload_id, did, content, created_at
             |FROM payloads
             |WHERE did = $did
             |ORDER BY creation_order ASC
             |LIMIT $limit
             |""".stripMargin
    }
    query.query[Payload].to[List]
  }
}
