package io.iohk.atala.prism.cmanager.repositories.daos

import java.time.Instant
import java.util.UUID

import doobie.implicits._
import io.iohk.atala.prism.cmanager.models.requests.CreateStudent
import io.iohk.atala.prism.cmanager.models.Student
import io.iohk.atala.prism.console.models.{Contact, Institution, IssuerGroup}

object StudentsDAO {

  def createStudent(data: CreateStudent): doobie.ConnectionIO[Student] = {
    val studentId = Student.Id(UUID.randomUUID())
    val externalId = Contact.ExternalId.random()
    val createdAt = Instant.now()
    val connectionStatus: Student.ConnectionStatus = Student.ConnectionStatus.InvitationMissing

    sql"""
         |INSERT INTO contacts
         |  (contact_id, contact_data, created_at, connection_status, created_by, external_id)
         |VALUES
         |  ($studentId, jsonb_build_object('university_assigned_id', ${data.universityAssignedId},
         |   'full_name', ${data.fullName}, 'email', ${data.email}, 'admission_date', ${data.admissionDate}::DATE),
         |   $createdAt, $connectionStatus::TEXT::CONTACT_CONNECTION_STATUS_TYPE, ${data.issuer}, $externalId)
         |""".stripMargin.update.run
      .map(_ => Student.create(data, studentId, createdAt, connectionStatus))
  }

  def getStudentsBy(
      issuer: Institution.Id,
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
             |  FROM contacts
             |  WHERE contact_id = $lastSeen
             |)
             |SELECT contact_id, contact_data ->> 'university_assigned_id', contact_data ->> 'full_name', contact_data ->> 'email', (contact_data ->> 'admission_date')::DATE, created_at, connection_status::TEXT::STUDENT_CONNECTION_STATUS_TYPE, connection_token, connection_id
             |FROM CTE CROSS JOIN contacts
             |     JOIN contacts_per_group USING (contact_id)
             |     JOIN issuer_groups g USING (group_id)
             |WHERE contacts.created_by = $issuer AND
             |      (created_at > last_seen_time OR (created_at = last_seen_time AND contact_id > $lastSeen)) AND
             |      g.name = $group
             |ORDER BY created_at ASC, contact_id
             |LIMIT $limit
             |""".stripMargin
      case (Some(lastSeen), None) =>
        sql"""
             |WITH CTE AS (
             |  SELECT created_at AS last_seen_time
             |  FROM contacts
             |  WHERE contact_id = $lastSeen
             |)
             |SELECT contact_id, contact_data ->> 'university_assigned_id', contact_data ->> 'full_name', contact_data ->> 'email', contact_data ->> 'admission_date', created_at, connection_status::TEXT::STUDENT_CONNECTION_STATUS_TYPE, connection_token, connection_id
             |FROM CTE CROSS JOIN contacts
             |WHERE contacts.created_by = $issuer AND
             |      (created_at > last_seen_time OR (created_at = last_seen_time AND contact_id > $lastSeen))
             |ORDER BY created_at ASC, contact_id
             |LIMIT $limit
             |""".stripMargin
      case (None, Some(group)) =>
        sql"""
             |SELECT contact_id, contact_data ->> 'university_assigned_id', contact_data ->> 'full_name', contact_data ->> 'email', contact_data ->> 'admission_date', created_at, connection_status::TEXT::STUDENT_CONNECTION_STATUS_TYPE, connection_token, connection_id
             |FROM contacts
             |     JOIN contacts_per_group USING (contact_id)
             |     JOIN issuer_groups g USING (group_id)
             |WHERE contacts.created_by = $issuer AND
             |      g.name = $group
             |ORDER BY created_at ASC, contact_id
             |LIMIT $limit
             |""".stripMargin
      case (None, None) =>
        sql"""
             |SELECT contact_id, contact_data ->> 'university_assigned_id', contact_data ->> 'full_name', contact_data ->> 'email', contact_data ->> 'admission_date', created_at, connection_status::TEXT::STUDENT_CONNECTION_STATUS_TYPE, connection_token, connection_id
             |FROM contacts
             |WHERE created_by = $issuer
             |ORDER BY created_at ASC, contact_id
             |LIMIT $limit
             |""".stripMargin
    }
    query.query[Student].to[List]
  }

  def findStudent(issuerId: Institution.Id, studentId: Student.Id): doobie.ConnectionIO[Option[Student]] = {
    sql"""
         |SELECT contact_id, contact_data ->> 'university_assigned_id', contact_data ->> 'full_name', contact_data ->> 'email', contact_data ->> 'admission_date', created_at, connection_status::TEXT::STUDENT_CONNECTION_STATUS_TYPE, connection_token, connection_id
         |FROM contacts
         |WHERE contact_id = $studentId AND
         |      created_by = $issuerId
         |""".stripMargin.query[Student].option
  }
}
