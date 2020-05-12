package io.iohk.cvp.cmanager.repositories.daos

import java.time.Instant
import java.util.UUID

import doobie.implicits._
import io.iohk.connector.model.{ConnectionId, TokenString}
import io.iohk.cvp.cmanager.models.requests.CreateStudent
import io.iohk.cvp.cmanager.models.{Issuer, IssuerGroup, Student}

object IssuerSubjectsDAO {

  import io.iohk.connector.repositories.daos._

  sealed trait UpdateSubjectRequest
  object UpdateSubjectRequest {
    final case class ConnectionTokenGenerated(studentId: Student.Id, token: TokenString) extends UpdateSubjectRequest
    final case class ConnectionAccepted(token: TokenString, connectionId: ConnectionId) extends UpdateSubjectRequest
  }

  def create(data: CreateStudent, groupId: IssuerGroup.Id): doobie.ConnectionIO[Student] = {
    val id = Student.Id(UUID.randomUUID())
    val createdAt = Instant.now()
    val connectionStatus: Student.ConnectionStatus = Student.ConnectionStatus.InvitationMissing

    sql"""
         |INSERT INTO issuer_subjects
         |  (subject_id, subject_data, created_at, connection_status, group_id)
         |VALUES
         |  ($id, jsonb_build_object('university_assigned_id', ${data.universityAssignedId},
         |   'full_name', ${data.fullName}, 'email', ${data.email}, 'admission_date', ${data.admissionDate}::DATE),
         |   $createdAt, $connectionStatus::STUDENT_CONNECTION_STATUS_TYPE, $groupId)
         |""".stripMargin.update.run
      .map(_ => Student.create(data, id, createdAt, connectionStatus))
  }

  def getBy(
      issuer: Issuer.Id,
      limit: Int,
      lastSeenStudent: Option[Student.Id],
      groupName: Option[IssuerGroup.Name]
  ): doobie.ConnectionIO[List[Student]] = {

    // TODO: Refactor to a single or at most two queries, likely using doobie fragments
    val query = (lastSeenStudent, groupName) match {
      case (Some(lastSeen), Some(group)) =>
        sql"""
             |WITH CTE AS (
             |  SELECT created_at AS last_seen_time
             |  FROM issuer_subjects
             |  WHERE subject_id = $lastSeen
             |)
             |SELECT subject_id, subject_data ->> 'university_assigned_id', subject_data ->> 'full_name', subject_data ->> 'email', (subject_data ->> 'admission_date')::DATE, created_at, connection_status, connection_token, connection_id, g.name
             |FROM CTE CROSS JOIN issuer_subjects JOIN issuer_groups g USING (group_id)
             |WHERE issuer_id = $issuer AND
             |      (created_at > last_seen_time OR (created_at = last_seen_time AND subject_id > $lastSeen)) AND
             |      g.name = $group
             |ORDER BY created_at ASC, subject_id
             |LIMIT $limit
             |""".stripMargin
      case (Some(lastSeen), None) =>
        sql"""
             |WITH CTE AS (
             |  SELECT created_at AS last_seen_time
             |  FROM issuer_subjects
             |  WHERE subject_id = $lastSeen
             |)
             |SELECT subject_id, subject_data ->> 'university_assigned_id', subject_data ->> 'full_name', subject_data ->> 'email', subject_data ->> 'admission_date', created_at, connection_status, connection_token, connection_id, g.name
             |FROM CTE CROSS JOIN issuer_subjects JOIN issuer_groups g USING (group_id)
             |WHERE issuer_id = $issuer AND
             |      (created_at > last_seen_time OR (created_at = last_seen_time AND subject_id > $lastSeen))
             |ORDER BY created_at ASC, subject_id
             |LIMIT $limit
             |""".stripMargin
      case (None, Some(group)) =>
        sql"""
             |SELECT subject_id, subject_data ->> 'university_assigned_id', subject_data ->> 'full_name', subject_data ->> 'email', subject_data ->> 'admission_date', created_at, connection_status, connection_token, connection_id, g.name
             |FROM issuer_subjects JOIN issuer_groups g USING (group_id)
             |WHERE issuer_id = $issuer AND
             |      g.name = $group
             |ORDER BY created_at ASC, subject_id
             |LIMIT $limit
             |""".stripMargin
      case (None, None) =>
        sql"""
             |SELECT subject_id, subject_data ->> 'university_assigned_id', subject_data ->> 'full_name', subject_data ->> 'email', subject_data ->> 'admission_date', created_at, connection_status, connection_token, connection_id, g.name
             |FROM issuer_subjects JOIN issuer_groups g USING (group_id)
             |WHERE issuer_id = $issuer
             |ORDER BY created_at ASC, subject_id
             |LIMIT $limit
             |""".stripMargin
    }
    query.query[Student].to[List]
  }

  def find(issuerId: Issuer.Id, studentId: Student.Id): doobie.ConnectionIO[Option[Student]] = {
    sql"""
         |SELECT subject_id, subject_data ->> 'university_assigned_id', subject_data ->> 'full_name', subject_data ->> 'email', subject_data ->> 'admission_date', created_at, connection_status, connection_token, connection_id, g.name
         |FROM issuer_subjects JOIN issuer_groups g USING (group_id)
         |WHERE subject_id = $studentId AND
         |      issuer_id = $issuerId
         |""".stripMargin.query[Student].option
  }

  def update(issuerId: Issuer.Id, request: UpdateSubjectRequest): doobie.ConnectionIO[Unit] = {
    request match {
      case UpdateSubjectRequest.ConnectionTokenGenerated(studentId, token) =>
        val status: Student.ConnectionStatus = Student.ConnectionStatus.ConnectionMissing
        sql"""
             |UPDATE issuer_subjects
             |SET connection_token = $token,
             |    connection_status = $status::STUDENT_CONNECTION_STATUS_TYPE
             |FROM issuer_groups
             |WHERE subject_id = $studentId AND
             |      issuer_id = $issuerId AND
             |      issuer_subjects.group_id = issuer_groups.group_id
              """.stripMargin.update.run.map(_ => ())
      case UpdateSubjectRequest.ConnectionAccepted(token, connectionId) =>
        val status: Student.ConnectionStatus = Student.ConnectionStatus.ConnectionAccepted
        // when the connection is accepted, we don't have the student id, just the token
        // TODO: Refactor the code to keep the subject_id and participant_id with the same value
        sql"""
             |UPDATE issuer_subjects
             |SET connection_id = $connectionId,
             |    connection_status = $status::STUDENT_CONNECTION_STATUS_TYPE
             |FROM issuer_groups
             |WHERE connection_token = $token AND
             |      issuer_id = $issuerId AND
             |      issuer_subjects.group_id = issuer_groups.group_id
              """.stripMargin.update.run.map(_ => ())
    }
  }
}
