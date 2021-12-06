package io.iohk.atala.prism.vault.repositories.daos

import java.time.Instant
import doobie.ConnectionIO
import doobie.implicits.legacy.instant._
import doobie.implicits.toSqlInterpolator
import io.iohk.atala.prism.vault.model.{CreateRecord, Record}

object RecordsDAO {
  def createRecord(data: CreateRecord): ConnectionIO[Record] = {
    val createdAt = Instant.now()

    sql"""
         |INSERT INTO records (record_type, record_id, payload, created_at)
         |VALUES (${data.type_}, ${data.id}, ${data.payload}, $createdAt)
         |RETURNING record_type, record_id, payload, created_at
         |""".stripMargin
      .query[Record]
      .unique
  }

  def getRecord(recordType: Record.Type, id: Record.Id): ConnectionIO[Option[Record]] = {
    sql"""
          SELECT FROM records (record_type, record_id, payload)
         |WHERE record_type = $recordType AND record_id = $id
         |""".stripMargin.query[Record].option
  }

  def getRecordsPaginated(
      recordType: Record.Type,
      lastSeenIdOpt: Option[Record.Id],
      limit: Int
  ): ConnectionIO[List[Record]] = {
    val query = lastSeenIdOpt match {
      case Some(lastSeenId) =>
        sql"""WITH CTE AS (
             |  SELECT created_at AS latest_seen_created_at
             |  FROM records
             |  WHERE record_id = $lastSeenId
             |)
             |SELECT record_type, record_id, payload
             |FROM CTE CROSS JOIN records
             |WHERE record_type = $recordType AND created_at > latest_seen_created_at
             |ORDER BY created_at ASC
             |LIMIT $limit
             |""".stripMargin
      case None =>
        sql"""SELECT record_type, record_id, payload
             |FROM records
             |WHERE record_type = $recordType
             |ORDER BY created_at ASC
             |LIMIT $limit
             |""".stripMargin
    }
    query.query[Record].to[List]
  }
}
