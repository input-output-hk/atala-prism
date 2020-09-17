package io.iohk.atala.prism.cmanager.repositories.daos

import java.time.Instant
import java.util.UUID

import doobie.implicits._
import io.iohk.atala.prism.connector.model.{ConnectionId, TokenString}
import io.iohk.atala.prism.cmanager.models.requests.{CreateStudent, CreateSubject}
import io.iohk.atala.prism.cmanager.models.{Issuer, IssuerGroup, Student, Subject}

object IssuerSubjectsDAO {

  sealed trait UpdateSubjectRequest
  object UpdateSubjectRequest {
    final case class ConnectionTokenGenerated(subjectId: Subject.Id, token: TokenString) extends UpdateSubjectRequest
    final case class ConnectionAccepted(token: TokenString, connectionId: ConnectionId) extends UpdateSubjectRequest
  }

  def createStudent(data: CreateStudent): doobie.ConnectionIO[Student] = {
    val studentId = Student.Id(UUID.randomUUID())
    val externalId = Subject.ExternalId.random()
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

  def create(data: CreateSubject): doobie.ConnectionIO[Subject] = {
    val subjectId = Subject.Id(UUID.randomUUID())
    val createdAt = Instant.now()
    val connectionStatus: Student.ConnectionStatus = Student.ConnectionStatus.InvitationMissing
    sql"""
         |INSERT INTO contacts
         |  (contact_id, contact_data, created_at, connection_status, created_by, external_id)
         |VALUES
         |  ($subjectId, ${data.data}, $createdAt, $connectionStatus::TEXT::CONTACT_CONNECTION_STATUS_TYPE,
         |   ${data.issuerId}, ${data.externalId})
         |""".stripMargin.update.run
      .map(_ => Subject.create(data, subjectId, createdAt, connectionStatus))
  }

  def getStudentsBy(
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

  def findStudent(issuerId: Issuer.Id, studentId: Student.Id): doobie.ConnectionIO[Option[Student]] = {
    sql"""
         |SELECT contact_id, contact_data ->> 'university_assigned_id', contact_data ->> 'full_name', contact_data ->> 'email', contact_data ->> 'admission_date', created_at, connection_status::TEXT::STUDENT_CONNECTION_STATUS_TYPE, connection_token, connection_id
         |FROM contacts
         |WHERE contact_id = $studentId AND
         |      created_by = $issuerId
         |""".stripMargin.query[Student].option
  }

  def findSubject(issuerId: Issuer.Id, subjectId: Subject.Id): doobie.ConnectionIO[Option[Subject]] = {
    sql"""
         |SELECT contact_id, external_id, contact_data, created_at, connection_status::TEXT::STUDENT_CONNECTION_STATUS_TYPE, connection_token, connection_id
         |FROM contacts
         |WHERE contact_id = $subjectId AND
         |      created_by = $issuerId
         |""".stripMargin.query[Subject].option
  }

  def findSubject(issuerId: Issuer.Id, externalId: Subject.ExternalId): doobie.ConnectionIO[Option[Subject]] = {
    sql"""
         |SELECT contact_id, external_id, contact_data, created_at, connection_status::TEXT::STUDENT_CONNECTION_STATUS_TYPE, connection_token, connection_id
         |FROM contacts
         |WHERE external_id = $externalId AND
         |      created_by = $issuerId
         |""".stripMargin.query[Subject].option
  }

  def update(issuerId: Issuer.Id, request: UpdateSubjectRequest): doobie.ConnectionIO[Unit] = {
    request match {
      case UpdateSubjectRequest.ConnectionTokenGenerated(subjectId, token) =>
        val status: Student.ConnectionStatus = Student.ConnectionStatus.ConnectionMissing
        sql"""
             |UPDATE contacts
             |SET connection_token = $token,
             |    connection_status = $status::TEXT::CONTACT_CONNECTION_STATUS_TYPE
             |WHERE contact_id = $subjectId AND
             |      created_by = $issuerId
              """.stripMargin.update.run.map(_ => ())
      case UpdateSubjectRequest.ConnectionAccepted(token, connectionId) =>
        val status: Student.ConnectionStatus = Student.ConnectionStatus.ConnectionAccepted
        // when the connection is accepted, we don't have the student id, just the token
        // TODO: Refactor the code to keep the subject_id and participant_id with the same value
        sql"""
             |UPDATE contacts
             |SET connection_id = $connectionId,
             |    connection_status = $status::TEXT::CONTACT_CONNECTION_STATUS_TYPE
             |WHERE connection_token = $token AND
             |      created_by = $issuerId
              """.stripMargin.update.run.map(_ => ())
    }
  }

  def getBy(
      issuerId: Issuer.Id,
      limit: Int,
      lastSeenSubject: Option[Subject.Id],
      groupName: Option[IssuerGroup.Name]
  ): doobie.ConnectionIO[List[Subject]] = {

    // TODO: Refactor to a single or at most two queries, likely using doobie fragments
    val query = (lastSeenSubject, groupName) match {
      case (Some(lastSeen), Some(group)) =>
        sql"""
             |WITH CTE AS (
             |  SELECT created_at AS last_seen_time
             |  FROM contacts
             |  WHERE contact_id = $lastSeen
             |)
             |SELECT contact_id, external_id, contact_data, created_at, connection_status::TEXT::STUDENT_CONNECTION_STATUS_TYPE, connection_token, connection_id
             |FROM CTE CROSS JOIN contacts
             |     JOIN contacts_per_group USING (contact_id)
             |     JOIN issuer_groups g USING (group_id)
             |WHERE contacts.created_by = $issuerId AND
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
             |SELECT contact_id, external_id, contact_data, created_at, connection_status::TEXT::STUDENT_CONNECTION_STATUS_TYPE, connection_token, connection_id
             |FROM CTE CROSS JOIN contacts
             |WHERE contacts.created_by = $issuerId AND
             |      (created_at > last_seen_time OR (created_at = last_seen_time AND contact_id > $lastSeen))
             |ORDER BY created_at ASC, contact_id
             |LIMIT $limit
             |""".stripMargin
      case (None, Some(group)) =>
        sql"""
             |SELECT contact_id, external_id, contact_data, created_at, connection_status::TEXT::STUDENT_CONNECTION_STATUS_TYPE, connection_token, connection_id
             |FROM contacts
             |     JOIN contacts_per_group USING (contact_id)
             |     JOIN issuer_groups g USING (group_id)
             |WHERE contacts.created_by = $issuerId AND
             |      g.name = $group
             |ORDER BY created_at ASC, contact_id
             |LIMIT $limit
             |""".stripMargin
      case (None, None) =>
        sql"""
             |SELECT contact_id, external_id, contact_data, created_at, connection_status::TEXT::STUDENT_CONNECTION_STATUS_TYPE, connection_token, connection_id
             |FROM contacts
             |WHERE created_by = $issuerId
             |ORDER BY created_at ASC, contact_id
             |LIMIT $limit
             |""".stripMargin
    }
    query.query[Subject].to[List]
  }
}
