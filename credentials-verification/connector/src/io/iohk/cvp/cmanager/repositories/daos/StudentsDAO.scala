package io.iohk.cvp.cmanager.repositories.daos

import java.time.Instant
import java.util.UUID

import doobie.implicits._
import io.iohk.connector.model.TokenString
import io.iohk.cvp.cmanager.models.requests.CreateStudent
import io.iohk.cvp.cmanager.models.{Issuer, Student}

object StudentsDAO {

  sealed trait UpdateStudentRequest
  object UpdateStudentRequest {
    final case class ConnectionTokenGenerated(token: TokenString) extends UpdateStudentRequest
  }

  def create(data: CreateStudent): doobie.ConnectionIO[Student] = {
    val id = Student.Id(UUID.randomUUID())
    val createdOn = Instant.now()
    val connectionStatus: Student.ConnectionStatus = Student.ConnectionStatus.InvitationMissing

    sql"""
         |INSERT INTO students
         |  (student_id, issuer_id, university_assigned_id, full_name, email, admission_date, created_on, connection_status)
         |VALUES
         |  ($id, ${data.issuer}, ${data.universityAssignedId}, ${data.fullName}, ${data.email},
         |   ${data.admissionDate}, $createdOn, $connectionStatus::STUDENT_CONNECTION_STATUS_TYPE)
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
             |SELECT student_id, issuer_id, university_assigned_id, full_name, email, admission_date, created_on, connection_status, connection_token, connection_id
             |FROM CTE CROSS JOIN students
             |WHERE issuer_id = $issuer AND
             |      (created_on > last_seen_time OR (created_on = last_seen_time AND student_id > $lastSeen))
             |ORDER BY created_on ASC, student_id
             |LIMIT $limit
             |""".stripMargin
      case None =>
        sql"""
             |SELECT student_id, issuer_id, university_assigned_id, full_name, email, admission_date, created_on, connection_status, connection_token, connection_id
             |FROM students
             |WHERE issuer_id = $issuer
             |ORDER BY created_on ASC, student_id
             |LIMIT $limit
             |""".stripMargin
    }
    query.query[Student].to[List]
  }

  def find(issuerId: Issuer.Id, studentId: Student.Id): doobie.ConnectionIO[Option[Student]] = {
    sql"""
         |SELECT student_id, issuer_id, university_assigned_id, full_name, email, admission_date, created_on, connection_status, connection_token, connection_id
         |FROM students
         |WHERE student_id = $studentId AND
         |      issuer_id = $issuerId
         |""".stripMargin.query[Student].option
  }

  def update(issuerId: Issuer.Id, studentId: Student.Id, request: UpdateStudentRequest): doobie.ConnectionIO[Unit] = {
    request match {
      case UpdateStudentRequest.ConnectionTokenGenerated(token) =>
        val status: Student.ConnectionStatus = Student.ConnectionStatus.ConnectionMissing
        sql"""
             |UPDATE students
             |SET connection_token = $token,
             |    connection_status = $status::STUDENT_CONNECTION_STATUS_TYPE
             |WHERE student_id = $studentId AND
             |      issuer_id = $issuerId
              """.stripMargin.update.run.map(_ => ())
    }
  }
}
