package io.iohk.cvp.cmanager.repositories.daos

import java.time.Instant
import java.util.UUID

import doobie.implicits._
import io.iohk.connector.model.{ConnectionId, TokenString}
import io.iohk.cvp.cmanager.models.requests.CreateStudent
import io.iohk.cvp.cmanager.models.{Issuer, IssuerGroup, Student}

object StudentsDAO {

  import io.iohk.connector.repositories.daos._

  sealed trait UpdateStudentRequest
  object UpdateStudentRequest {
    final case class ConnectionTokenGenerated(studentId: Student.Id, token: TokenString) extends UpdateStudentRequest
    final case class ConnectionAccepted(token: TokenString, connectionId: ConnectionId) extends UpdateStudentRequest
  }

  def create(data: CreateStudent, groupId: IssuerGroup.Id): doobie.ConnectionIO[Student] = {
    val id = Student.Id(UUID.randomUUID())
    val createdOn = Instant.now()
    val connectionStatus: Student.ConnectionStatus = Student.ConnectionStatus.InvitationMissing

    sql"""
         |INSERT INTO students
         |  (student_id, university_assigned_id, full_name, email, admission_date, created_on, connection_status, group_id)
         |VALUES
         |  ($id, ${data.universityAssignedId}, ${data.fullName}, ${data.email},
         |   ${data.admissionDate}, $createdOn, $connectionStatus::STUDENT_CONNECTION_STATUS_TYPE, $groupId)
         |""".stripMargin.update.run
      .map(_ => Student.create(data, id, createdOn, connectionStatus))
  }

  def getBy(issuer: Issuer.Id, limit: Int, lastSeenStudent: Option[Student.Id]): doobie.ConnectionIO[List[Student]] = {
    val query = lastSeenStudent match {
      case Some(lastSeen) =>
        sql"""
             |WITH CTE AS (
             |  SELECT created_on AS last_seen_time
             |  FROM students
             |  WHERE student_id = $lastSeen
             |)
             |SELECT student_id, university_assigned_id, full_name, email, admission_date, created_on, connection_status, connection_token, connection_id, g.name
             |FROM CTE CROSS JOIN students JOIN issuer_groups g USING (group_id)
             |WHERE issuer_id = $issuer AND
             |      (created_on > last_seen_time OR (created_on = last_seen_time AND student_id > $lastSeen))
             |ORDER BY created_on ASC, student_id
             |LIMIT $limit
             |""".stripMargin
      case None =>
        sql"""
             |SELECT student_id, university_assigned_id, full_name, email, admission_date, created_on, connection_status, connection_token, connection_id, g.name
             |FROM students JOIN issuer_groups g USING (group_id)
             |WHERE issuer_id = $issuer
             |ORDER BY created_on ASC, student_id
             |LIMIT $limit
             |""".stripMargin
    }
    query.query[Student].to[List]
  }

  def find(issuerId: Issuer.Id, studentId: Student.Id): doobie.ConnectionIO[Option[Student]] = {
    sql"""
         |SELECT student_id, university_assigned_id, full_name, email, admission_date, created_on, connection_status, connection_token, connection_id, g.name
         |FROM students JOIN issuer_groups g USING (group_id)
         |WHERE student_id = $studentId AND
         |      issuer_id = $issuerId
         |""".stripMargin.query[Student].option
  }

  def update(issuerId: Issuer.Id, request: UpdateStudentRequest): doobie.ConnectionIO[Unit] = {
    request match {
      case UpdateStudentRequest.ConnectionTokenGenerated(studentId, token) =>
        val status: Student.ConnectionStatus = Student.ConnectionStatus.ConnectionMissing
        sql"""
             |UPDATE students
             |SET connection_token = $token,
             |    connection_status = $status::STUDENT_CONNECTION_STATUS_TYPE
             |FROM issuer_groups
             |WHERE student_id = $studentId AND
             |      issuer_id = $issuerId AND
             |      students.group_id = issuer_groups.group_id
              """.stripMargin.update.run.map(_ => ())
      case UpdateStudentRequest.ConnectionAccepted(token, connectionId) =>
        val status: Student.ConnectionStatus = Student.ConnectionStatus.ConnectionAccepted
        // when the connection is accepted, we don't have the student id, just the token
        // TODO: Refactor the code to keep the student_id and participant_id with the same value
        sql"""
             |UPDATE students
             |SET connection_id = $connectionId,
             |    connection_status = $status::STUDENT_CONNECTION_STATUS_TYPE
             |FROM issuer_groups
             |WHERE connection_token = $token AND
             |      issuer_id = $issuerId AND
             |      students.group_id = issuer_groups.group_id
              """.stripMargin.update.run.map(_ => ())
    }
  }
}
