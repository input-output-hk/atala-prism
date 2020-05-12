package io.iohk.cvp.cmanager.repositories.daos

import java.time.Instant
import java.util.UUID

import doobie.implicits._
import io.iohk.cvp.cmanager.models.requests.CreateCredential
import io.iohk.cvp.cmanager.models.{Credential, Issuer, Student}

object CredentialsDAO {

  def create(data: CreateCredential): doobie.ConnectionIO[Credential] = {
    val id = Credential.Id(UUID.randomUUID())
    val createdOn = Instant.now()
    sql"""
         |WITH inserted AS (
         |  INSERT INTO credentials (credential_id, issuer_id, subject_id, title, enrollment_date, graduation_date, group_name, created_on)
         |  VALUES ($id, ${data.issuedBy}, ${data.studentId}, ${data.title}, ${data.enrollmentDate}, ${data.graduationDate}, ${data.groupName}, $createdOn)
         |  RETURNING credential_id, issuer_id, subject_id, title, enrollment_date, graduation_date, group_name, created_on
         |)
         | , PTS AS (
         |  SELECT id AS issuer_id, name
         |  FROM participants
         |  WHERE tpe = 'issuer'::PARTICIPANT_TYPE
         |)
         |SELECT inserted.*, PTS.name AS issuer_name, issuer_subjects.subject_data ->> 'full_name' AS student_name
         |FROM inserted
         |     JOIN PTS USING (issuer_id)
         |     JOIN issuer_subjects USING (subject_id)
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
             | , PTS AS (
             |  SELECT id AS issuer_id, name
             |  FROM participants
             |  WHERE tpe = 'issuer'::PARTICIPANT_TYPE
             |)
             |SELECT credential_id, c.issuer_id, c.subject_id, title, enrollment_date, graduation_date, group_name, c.created_on, PTS.name AS issuer_name, issuer_subjects.subject_data ->> 'full_name' AS student_name
             |FROM CTE CROSS JOIN credentials c
             |     JOIN PTS USING (issuer_id)
             |     JOIN issuer_subjects USING (subject_id)
             |WHERE c.issuer_id = $issuedBy AND
             |      (c.created_on > last_seen_time OR (c.created_on = last_seen_time AND credential_id > $lastSeen))
             |ORDER BY c.created_on ASC, credential_id
             |LIMIT $limit
             |""".stripMargin
      case None =>
        sql"""
             |WITH PTS AS (
             |  SELECT id AS issuer_id, name
             |  FROM participants
             |  WHERE tpe = 'issuer'::PARTICIPANT_TYPE
             |)
             |SELECT credential_id, c.issuer_id, c.subject_id, title, enrollment_date, graduation_date, group_name, c.created_on, PTS.name AS issuer_name, issuer_subjects.subject_data ->> 'full_name' AS student_name
             |FROM credentials c
             |     JOIN PTS USING (issuer_id)
             |     JOIN issuer_subjects USING (subject_id)
             |WHERE c.issuer_id = $issuedBy
             |ORDER BY c.created_on ASC, credential_id
             |LIMIT $limit
             |""".stripMargin
    }
    query.query[Credential].to[List]
  }

  def getBy(issuedBy: Issuer.Id, studentId: Student.Id): doobie.ConnectionIO[List[Credential]] = {
    sql"""
         |WITH PTS AS (
         |  SELECT id AS issuer_id, name
         |  FROM participants
         |  WHERE tpe = 'issuer'::PARTICIPANT_TYPE
         |)
         |SELECT credential_id, c.issuer_id, c.subject_id, title, enrollment_date, graduation_date, group_name, c.created_on, PTS.name AS issuer_name, issuer_subjects.subject_data ->> 'full_name' AS student_name
         |FROM credentials c
         |     JOIN PTS USING (issuer_id)
         |     JOIN issuer_subjects USING (subject_id)
         |WHERE c.issuer_id = $issuedBy AND
         |      c.subject_id = $studentId
         |ORDER BY c.created_on ASC, credential_id
         |""".stripMargin.query[Credential].to[List]
  }
}
