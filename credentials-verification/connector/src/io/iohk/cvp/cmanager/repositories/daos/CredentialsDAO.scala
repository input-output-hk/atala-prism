package io.iohk.cvp.cmanager.repositories.daos

import java.time.Instant
import java.util.UUID

import doobie.implicits._
import io.iohk.cvp.cmanager.models.requests.CreateCredential
import io.iohk.cvp.cmanager.models.{Credential, Issuer}

object CredentialsDAO {

  def create(data: CreateCredential): doobie.ConnectionIO[Credential] = {
    val id = Credential.Id(UUID.randomUUID())
    val createdOn = Instant.now()
    sql"""
         |WITH inserted AS (
         |  INSERT INTO credentials (credential_id, issuer_id, student_id, title, enrollment_date, graduation_date, group_name, created_on)
         |  VALUES ($id, ${data.issuedBy}, ${data.studentId}, ${data.title}, ${data.enrollmentDate}, ${data.graduationDate}, ${data.groupName}, $createdOn)
         |  RETURNING credential_id, issuer_id, student_id, title, enrollment_date, graduation_date, group_name, created_on
         |)
         |SELECT inserted.*, issuers.name AS issuer_name, students.full_name AS student_name
         |FROM inserted
         |     JOIN issuers USING (issuer_id)
         |     JOIN students USING (student_id)
         |""".stripMargin.query[Credential].unique
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
             |SELECT credential_id, c.issuer_id, student_id, title, enrollment_date, graduation_date, group_name, c.created_on, issuers.name AS issuer_name, students.full_name AS student_name
             |FROM CTE CROSS JOIN credentials c
             |     JOIN issuers USING (issuer_id)
             |     JOIN students USING (student_id)
             |WHERE c.issuer_id = $issuedBy AND
             |      (c.created_on > last_seen_time OR (c.created_on = last_seen_time AND credential_id > $lastSeen))
             |ORDER BY c.created_on ASC, credential_id
             |LIMIT $limit
             |""".stripMargin
      case None =>
        sql"""
             |SELECT credential_id, c.issuer_id, student_id, title, enrollment_date, graduation_date, group_name, c.created_on, issuers.name AS issuer_name, students.full_name AS student_name
             |FROM credentials c
             |     JOIN issuers USING (issuer_id)
             |     JOIN students USING (student_id)
             |WHERE c.issuer_id = $issuedBy
             |ORDER BY c.created_on ASC, credential_id
             |LIMIT $limit
             |""".stripMargin
    }
    query.query[Credential].to[List]
  }
}
