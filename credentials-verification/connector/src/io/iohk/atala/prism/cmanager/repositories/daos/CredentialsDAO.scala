package io.iohk.atala.prism.cmanager.repositories.daos

import java.time.Instant
import java.util.UUID

import doobie._
import cats.implicits._
import doobie.implicits._
import io.iohk.atala.prism.cmanager.models.requests.{
  CreateGenericCredential,
  CreateUniversityCredential,
  PublishCredential
}
import io.iohk.atala.prism.cmanager.models.{GenericCredential, Issuer, Student, Subject, UniversityCredential}

object CredentialsDAO {

  def createUniversityCredential(data: CreateUniversityCredential): doobie.ConnectionIO[UniversityCredential] = {
    val id = UniversityCredential.Id(UUID.randomUUID())
    val createdOn = Instant.now()
    sql"""
         |WITH inserted AS (
         |  INSERT INTO credentials (credential_id, issuer_id, subject_id, credential_data, group_name, created_on)
         |  VALUES ($id, ${data.issuedBy}, ${data.studentId}, jsonb_build_object('title', ${data.title}, 'enrollment_date', ${data.enrollmentDate}::DATE, 'graduation_date', ${data.graduationDate}::DATE), ${data.groupName}, $createdOn)
         |  RETURNING credential_id, issuer_id, subject_id, credential_data ->> 'title', (credential_data ->> 'enrollment_date')::DATE, (credential_data ->> 'graduation_date')::DATE, group_name, created_on
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
         |""".stripMargin.query[UniversityCredential].unique
  }

  def getUniversityCredentialsBy(
      issuedBy: Issuer.Id,
      limit: Int,
      lastSeenCredential: Option[UniversityCredential.Id]
  ): doobie.ConnectionIO[List[UniversityCredential]] = {
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
             |SELECT credential_id, c.issuer_id, c.subject_id, credential_data ->> 'title', (credential_data ->> 'enrollment_date')::DATE, (credential_data ->> 'graduation_date')::DATE, group_name, c.created_on, PTS.name AS issuer_name, issuer_subjects.subject_data ->> 'full_name' AS student_name
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
             |SELECT credential_id, c.issuer_id, c.subject_id, credential_data ->> 'title', (credential_data ->> 'enrollment_date')::DATE, (credential_data ->> 'graduation_date')::DATE, group_name, c.created_on, PTS.name AS issuer_name, issuer_subjects.subject_data ->> 'full_name' AS student_name
             |FROM credentials c
             |     JOIN PTS USING (issuer_id)
             |     JOIN issuer_subjects USING (subject_id)
             |WHERE c.issuer_id = $issuedBy
             |ORDER BY c.created_on ASC, credential_id
             |LIMIT $limit
             |""".stripMargin
    }
    query.query[UniversityCredential].to[List]
  }

  def getUniversityCredentialsBy(
      issuedBy: Issuer.Id,
      studentId: Student.Id
  ): doobie.ConnectionIO[List[UniversityCredential]] = {
    sql"""
         |WITH PTS AS (
         |  SELECT id AS issuer_id, name
         |  FROM participants
         |  WHERE tpe = 'issuer'::PARTICIPANT_TYPE
         |)
         |SELECT credential_id, c.issuer_id, c.subject_id, credential_data ->> 'title', (credential_data ->> 'enrollment_date')::DATE, (credential_data ->> 'graduation_date')::DATE, group_name, c.created_on, PTS.name AS issuer_name, issuer_subjects.subject_data ->> 'full_name' AS student_name
         |FROM credentials c
         |     JOIN PTS USING (issuer_id)
         |     JOIN issuer_subjects USING (subject_id)
         |WHERE c.issuer_id = $issuedBy AND
         |      c.subject_id = $studentId
         |ORDER BY c.created_on ASC, credential_id
         |""".stripMargin.query[UniversityCredential].to[List]
  }

  // Generic credentials methods
  def create(data: CreateGenericCredential): doobie.ConnectionIO[GenericCredential] = {
    val id = UniversityCredential.Id(UUID.randomUUID())
    val createdOn = Instant.now()
    sql"""
         |WITH inserted AS (
         |  INSERT INTO credentials (credential_id, issuer_id, subject_id, credential_data, group_name, created_on)
         |  VALUES ($id, ${data.issuedBy}, ${data.subjectId}, ${data.credentialData}, ${data.groupName}, $createdOn)
         |  RETURNING credential_id, issuer_id, subject_id, credential_data, group_name, created_on
         |)
         | , PTS AS (
         |  SELECT id AS issuer_id, name
         |  FROM participants
         |  WHERE tpe = 'issuer'::PARTICIPANT_TYPE
         |)
         |SELECT inserted.*, issuer_subjects.external_id, PTS.name AS issuer_name, issuer_subjects.subject_data, pc.*
         |FROM inserted
         |     JOIN PTS USING (issuer_id)
         |     JOIN issuer_subjects USING (subject_id)
         |     LEFT JOIN published_credentials pc USING (credential_id)
         |""".stripMargin.query[GenericCredential].unique
  }

  def getBy(
      issuedBy: Issuer.Id,
      limit: Int,
      lastSeenCredential: Option[GenericCredential.Id]
  ): doobie.ConnectionIO[List[GenericCredential]] = {
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
             |SELECT credential_id, c.issuer_id, c.subject_id, credential_data, group_name, c.created_on,
             |       issuer_subjects.external_id, PTS.name AS issuer_name, issuer_subjects.subject_data,
             |       pc.node_credential_id, pc.operation_hash, pc.encoded_signed_credential, pc.stored_at
             |FROM CTE CROSS JOIN credentials c
             |     JOIN PTS USING (issuer_id)
             |     JOIN issuer_subjects USING (subject_id)
             |     LEFT JOIN published_credentials pc USING (credential_id)
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
             |SELECT credential_id, c.issuer_id, c.subject_id, credential_data, group_name, c.created_on,
             |       issuer_subjects.external_id, PTS.name AS issuer_name, issuer_subjects.subject_data,
             |       pc.node_credential_id, pc.operation_hash, pc.encoded_signed_credential, pc.stored_at
             |FROM credentials c
             |     JOIN PTS USING (issuer_id)
             |     JOIN issuer_subjects USING (subject_id)
             |     LEFT JOIN published_credentials pc USING (credential_id)
             |WHERE c.issuer_id = $issuedBy
             |ORDER BY c.created_on ASC, credential_id
             |LIMIT $limit
             |""".stripMargin
    }
    query.query[GenericCredential].to[List]
  }

  def getBy(issuedBy: Issuer.Id, subjectId: Subject.Id): doobie.ConnectionIO[List[GenericCredential]] = {
    sql"""
         |WITH PTS AS (
         |  SELECT id AS issuer_id, name
         |  FROM participants
         |  WHERE tpe = 'issuer'::PARTICIPANT_TYPE
         |)
         |SELECT credential_id, c.issuer_id, c.subject_id, credential_data, group_name, c.created_on,
         |       issuer_subjects.external_id, PTS.name AS issuer_name, issuer_subjects.subject_data,
         |       pc.node_credential_id, pc.operation_hash, pc.encoded_signed_credential, pc.stored_at
         |FROM credentials c
         |     JOIN PTS USING (issuer_id)
         |     JOIN issuer_subjects USING (subject_id)
         |     LEFT JOIN published_credentials pc USING (credential_id)
         |WHERE c.issuer_id = $issuedBy AND
         |      c.subject_id = $subjectId
         |ORDER BY c.created_on ASC, credential_id
         |""".stripMargin.query[GenericCredential].to[List]
  }

  def storePublicationData(issuerId: Issuer.Id, credentialData: PublishCredential): doobie.ConnectionIO[Int] = {
    sql"""
         | INSERT INTO published_credentials (credential_id, node_credential_id, operation_hash, encoded_signed_credential, stored_at)
         | SELECT credential_id, ${credentialData.nodeCredentialId}, ${credentialData.issuanceOperationHash}, ${credentialData.encodedSignedCredential}, now()
         | FROM credentials
         | WHERE credential_id = ${credentialData.credentialId} AND
         |       issuer_id = $issuerId
         |""".stripMargin.update.run.flatTap { n =>
      FC.raiseError(new RuntimeException(s"The credential was not issued by the specified issuer")).whenA(n != 1)
    }
  }
}
