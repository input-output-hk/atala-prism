package io.iohk.atala.prism.console.repositories
package daos

import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.implicits.legacy.instant._
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.console.models._
import io.iohk.atala.prism.daos.BaseDAO

import java.time.Instant

object CredentialsDAO extends BaseDAO {

  def create(data: CreateGenericCredential): doobie.ConnectionIO[GenericCredential] = {
    val id = GenericCredential.Id.random()
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
         |), PC AS (
         |  SELECT credential_id, batch_id, issuance_operation_hash, issuance_operation_id, encoded_signed_credential, inclusion_proof,
         |         stored_at, shared_at, revoked_on_operation_id
         |  FROM published_credentials JOIN published_batches USING (batch_id)
         |  WHERE credential_id = $id
         |)
         |SELECT inserted.*, contacts.external_id, PTS.name AS issuer_name, contacts.contact_data, connection_status,
         |       PC.batch_id, PC.issuance_operation_hash, PC.issuance_operation_id, PC.encoded_signed_credential, PC.inclusion_proof,
         |       PC.stored_at, PC.shared_at, PC.revoked_on_operation_id
         |FROM inserted
         |     JOIN PTS USING (issuer_id)
         |     JOIN contacts ON (inserted.subject_id = contacts.contact_id)
         |     LEFT JOIN PC USING (credential_id)
         |""".stripMargin.query[GenericCredential].unique
  }

  def getBy(credentialId: GenericCredential.Id): doobie.ConnectionIO[Option[GenericCredential]] = {
    sql"""
         |WITH PTS AS (
         |  SELECT id AS issuer_id, name
         |  FROM participants
         |  WHERE tpe = 'issuer'::PARTICIPANT_TYPE
         |), PC AS (
         |  SELECT credential_id, batch_id, issuance_operation_hash, issuance_operation_id, encoded_signed_credential, inclusion_proof,
         |         stored_at, shared_at, revoked_on_operation_id
         |  FROM published_credentials JOIN published_batches USING (batch_id)
         |  WHERE credential_id = $credentialId
         |)
         |SELECT credential_id, c.issuer_id, c.subject_id, credential_data, group_name, c.created_on,
         |       external_id, PTS.name AS issuer_name, contact_data, connection_status,
         |       PC.batch_id, PC.issuance_operation_hash, PC.issuance_operation_id, PC.encoded_signed_credential, PC.inclusion_proof,
         |       PC.stored_at, PC.shared_at, PC.revoked_on_operation_id
         |FROM credentials c
         |     JOIN PTS USING (issuer_id)
         |     JOIN contacts ON (c.subject_id = contacts.contact_id)
         |     LEFT JOIN PC USING (credential_id)
         |WHERE credential_id = $credentialId
         |""".stripMargin.query[GenericCredential].option
  }

  def getBy(
      issuedBy: Institution.Id,
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
             |), PC AS (
             |  SELECT credential_id, batch_id, issuance_operation_hash, issuance_operation_id, encoded_signed_credential, inclusion_proof,
             |         stored_at, shared_at, revoked_on_operation_id
             |  FROM published_credentials JOIN published_batches USING (batch_id)
             |  WHERE credential_id = $lastSeen
             |)
             |SELECT credential_id, c.issuer_id, c.subject_id, credential_data, group_name, c.created_on,
             |       external_id, PTS.name AS issuer_name, contact_data, connection_status,
             |       PC.batch_id, PC.issuance_operation_hash, PC.issuance_operation_id, PC.encoded_signed_credential, PC.inclusion_proof,
             |       PC.stored_at, PC.shared_at, PC.revoked_on_operation_id
             |FROM CTE CROSS JOIN credentials c
             |     JOIN PTS USING (issuer_id)
             |     JOIN contacts ON (c.subject_id = contacts.contact_id)
             |     LEFT JOIN PC USING (credential_id)
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
             |), PC AS (
             |  SELECT credential_id, batch_id, issuance_operation_hash, issuance_operation_id, encoded_signed_credential, inclusion_proof,
             |         stored_at, shared_at, revoked_on_operation_id
             |  FROM published_credentials JOIN published_batches USING (batch_id)
             |)
             |SELECT credential_id, c.issuer_id, c.subject_id, credential_data, group_name, c.created_on,
             |       external_id, PTS.name AS issuer_name, contact_data, connection_status,
             |       PC.batch_id, PC.issuance_operation_hash, PC.issuance_operation_id, PC.encoded_signed_credential, PC.inclusion_proof,
             |       PC.stored_at, PC.shared_at, PC.revoked_on_operation_id
             |FROM credentials c
             |     JOIN PTS USING (issuer_id)
             |     JOIN contacts ON (c.subject_id = contacts.contact_id)
             |     LEFT JOIN PC USING (credential_id)
             |WHERE c.issuer_id = $issuedBy
             |ORDER BY c.created_on ASC, credential_id
             |LIMIT $limit
             |""".stripMargin
    }
    query.query[GenericCredential].to[List]
  }

  def getBy(
      issuedBy: Institution.Id,
      limit: Int,
      offset: Int
  ): doobie.ConnectionIO[List[GenericCredential]] = {
    sql"""
         |WITH PTS AS (
         |  SELECT id AS issuer_id, name
         |  FROM participants
         |  WHERE tpe = 'issuer'::PARTICIPANT_TYPE
         |), PC AS (
         |  SELECT credential_id, batch_id, issuance_operation_hash, issuance_operation_id, encoded_signed_credential, inclusion_proof,
         |         stored_at, shared_at, revoked_on_operation_id
         |  FROM published_credentials JOIN published_batches USING (batch_id)
         |)
         |SELECT credential_id, c.issuer_id, c.subject_id, credential_data, group_name, c.created_on,
         |       external_id, PTS.name AS issuer_name, contact_data, connection_status,
         |       PC.batch_id, PC.issuance_operation_hash, PC.issuance_operation_id, PC.encoded_signed_credential, PC.inclusion_proof,
         |       PC.stored_at, PC.shared_at, PC.revoked_on_operation_id
         |FROM credentials c
         |     JOIN PTS USING (issuer_id)
         |     JOIN contacts ON (c.subject_id = contacts.contact_id)
         |     LEFT JOIN PC USING (credential_id)
         |WHERE c.issuer_id = $issuedBy
         |ORDER BY c.created_on ASC, credential_id
         |LIMIT $limit
         |OFFSET $offset
         |""".stripMargin.query[GenericCredential].to[List]
  }

  def getBy(issuedBy: Institution.Id, subjectId: Contact.Id): doobie.ConnectionIO[List[GenericCredential]] = {
    sql"""
         |WITH PTS AS (
         |  SELECT id AS issuer_id, name
         |  FROM participants
         |  WHERE tpe = 'issuer'::PARTICIPANT_TYPE
         |), PC AS (
         |  SELECT credential_id, batch_id, issuance_operation_hash, issuance_operation_id, encoded_signed_credential, inclusion_proof,
         |         stored_at, shared_at, revoked_on_operation_id
         |  FROM published_credentials JOIN published_batches USING (batch_id)
         |)
         |SELECT credential_id, c.issuer_id, c.subject_id, credential_data, group_name, c.created_on,
         |       external_id, PTS.name AS issuer_name, contacts.contact_data, connection_status,
         |       PC.batch_id, PC.issuance_operation_hash, PC.issuance_operation_id, PC.encoded_signed_credential, PC.inclusion_proof,
         |       PC.stored_at, PC.shared_at, PC.revoked_on_operation_id
         |FROM credentials c
         |     JOIN PTS USING (issuer_id)
         |     JOIN contacts ON (c.subject_id = contacts.contact_id)
         |     LEFT JOIN PC USING (credential_id)
         |WHERE c.issuer_id = $issuedBy AND
         |      c.subject_id = $subjectId
         |ORDER BY c.created_on ASC, credential_id
         |""".stripMargin.query[GenericCredential].to[List]
  }

  def storeCredentialPublicationData(
      issuerId: Institution.Id,
      credentialData: CredentialPublicationData
  ): doobie.ConnectionIO[Int] = {
    sql"""
         |INSERT INTO published_credentials (
         |  credential_id, batch_id, encoded_signed_credential, inclusion_proof
         |)
         |SELECT credential_id,
         |       ${credentialData.credentialBatchId},
         |       ${credentialData.encodedSignedCredential},
         |       ${credentialData.proofOfInclusion}
         |FROM credentials
         |WHERE credential_id = ${credentialData.consoleCredentialId} AND
         |      issuer_id = $issuerId
         |""".stripMargin.update.run.flatTap { n =>
      FC.raiseError(new RuntimeException(s"The credential was not issued by the specified issuer")).whenA(n != 1)
    }
  }

  def storeBatchData(batchData: StoreBatchData): doobie.ConnectionIO[Int] = {
    sql"""
         |INSERT INTO published_batches (
         |  batch_id, issuance_operation_hash, issuance_operation_id, stored_at
         |)
         |VALUES (${batchData.batchId},
         |        ${batchData.issuanceOperationHash},
         |        ${batchData.atalaOperationId},
         |        ${Instant.now()}
         |)
         |""".stripMargin.update.run
  }

  def revokeCredential(
      institutionId: Institution.Id,
      credentialId: GenericCredential.Id,
      operationId: AtalaOperationId
  ): doobie.ConnectionIO[Unit] = {
    sql"""
         |WITH institution_credentials AS (
         |  SELECT credential_id
         |  FROM credentials
         |  WHERE credential_id = $credentialId AND
         |        issuer_id = $institutionId
         |)
         |UPDATE published_credentials
         |SET revoked_on_operation_id = $operationId
         |WHERE credential_id = $credentialId AND
         |      credential_id IN (SELECT * FROM institution_credentials)
         |""".stripMargin.update.run.flatTap { n =>
      FC.raiseError(new RuntimeException(s"The credential wasn't found or it hasn't been published yet"))
        .whenA(n != 1)
    }.void
  }

  def markAsShared(institutionId: Institution.Id, credentialId: GenericCredential.Id): doobie.ConnectionIO[Unit] = {
    sql"""
         |UPDATE published_credentials pc
         |SET shared_at = CURRENT_TIMESTAMP
         |FROM credentials c
         |WHERE c.credential_id = pc.credential_id AND
         |      pc.credential_id = $credentialId AND
         |      issuer_id = $institutionId
         |""".stripMargin.update.run.flatTap { n =>
      FC.raiseError(new RuntimeException(s"The credential wasn't found or it hasn't been published yet"))
        .whenA(n != 1)
    }.void
  }
}
