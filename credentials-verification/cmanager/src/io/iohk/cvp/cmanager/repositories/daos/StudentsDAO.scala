package io.iohk.cvp.cmanager.repositories.daos

import java.time.Instant
import java.util.UUID

import doobie.implicits._
import io.iohk.cvp.cmanager.models.Student
import io.iohk.cvp.cmanager.models.requests.CreateStudent

object StudentsDAO {
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
}
