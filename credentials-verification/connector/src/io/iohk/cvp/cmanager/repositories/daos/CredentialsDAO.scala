package io.iohk.cvp.cmanager.repositories.daos

import java.time.{Instant, OffsetDateTime}
import java.util.UUID

import doobie.postgres.implicits._
import doobie.implicits._
import io.iohk.cvp.cmanager.models.{Credential, Issuer}
import io.iohk.cvp.cmanager.models.requests.CreateCredential

object CredentialsDAO {

  def create(data: CreateCredential): doobie.ConnectionIO[Credential] = {
    val id = Credential.Id(UUID.randomUUID())
    val createdOn = Instant.now()
    sql"""
         |INSERT INTO credentials (credential_id, created_on, issued_by, subject, title, enrollment_date, graduation_date, group_name)
         |VALUES ($id, $createdOn, ${data.issuedBy}, ${data.subject}, ${data.title}, ${data.enrollmentDate}, ${data.graduationDate}, ${data.groupName})
         |""".stripMargin.update.run
      .map(_ => Credential.create(id, createdOn, data))
  }

  def getBy(
      issuedBy: Issuer.Id,
      limit: Int,
      lastSeenCredential: Option[Credential.Id]
  ): doobie.ConnectionIO[List[Credential]] = {
    val query = lastSeenCredential match {
      case Some(lastSeen) =>
        sql"""
             |WITH CTE AS (
             |  SELECT created_on AS last_seen_time
             |  FROM credentials
             |  WHERE credential_id = $lastSeen
             |)
             |SELECT credential_id, created_on, issued_by, subject, title, enrollment_date, graduation_date, group_name
             |FROM CTE CROSS JOIN credentials
             |WHERE issued_by = $issuedBy AND
             |      (created_on > last_seen_time OR (created_on = last_seen_time AND credential_id > $lastSeen))
             |ORDER BY created_on ASC, credential_id
             |LIMIT $limit
             |""".stripMargin
      case None =>
        sql"""
             |SELECT credential_id, created_on, issued_by, subject, title, enrollment_date, graduation_date, group_name
             |FROM credentials
             |WHERE issued_by = $issuedBy
             |ORDER BY created_on ASC, credential_id
             |LIMIT $limit
             |""".stripMargin
    }
    query.query[Credential].to[List]
  }
}
