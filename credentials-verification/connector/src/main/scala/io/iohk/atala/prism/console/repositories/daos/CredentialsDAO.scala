package io.iohk.atala.prism.console.repositories.daos

import java.time.Instant
import java.util.UUID

import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.implicits.legacy.instant._
import io.iohk.atala.prism.console.models.{
  Contact,
  CreateGenericCredential,
  GenericCredential,
  Institution,
  PublishCredential
}

object CredentialsDAO {
  import io.iohk.atala.prism.models.DoobieImplicits._

  def create(data: CreateGenericCredential): doobie.ConnectionIO[GenericCredential] = {
    val id = GenericCredential.Id(UUID.randomUUID())
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
         |SELECT inserted.*, contacts.external_id, PTS.name AS issuer_name, contacts.contact_data, connection_status,
         |       pc.node_credential_id, pc.operation_hash, pc.encoded_signed_credential, pc.stored_at,
         |       pc.transaction_id, pc.ledger, pc.shared_at
         |FROM inserted
         |     JOIN PTS USING (issuer_id)
         |     JOIN contacts ON (inserted.subject_id = contacts.contact_id)
         |     LEFT JOIN published_credentials pc USING (credential_id)
         |""".stripMargin.query[GenericCredential].unique
  }

  def getBy(credentialId: GenericCredential.Id): doobie.ConnectionIO[Option[GenericCredential]] = {
    sql"""
         |WITH PTS AS (
         |  SELECT id AS issuer_id, name
         |  FROM participants
         |  WHERE tpe = 'issuer'::PARTICIPANT_TYPE
         |)
         |SELECT credential_id, c.issuer_id, c.subject_id, credential_data, group_name, c.created_on,
         |       external_id, PTS.name AS issuer_name, contact_data, connection_status,
         |       pc.node_credential_id, pc.operation_hash, pc.encoded_signed_credential, pc.stored_at,
         |       pc.transaction_id, pc.ledger, pc.shared_at
         |FROM credentials c
         |     JOIN PTS USING (issuer_id)
         |     JOIN contacts ON (c.subject_id = contacts.contact_id)
         |     LEFT JOIN published_credentials pc USING (credential_id)
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
             |)
             |SELECT credential_id, c.issuer_id, c.subject_id, credential_data, group_name, c.created_on,
             |       external_id, PTS.name AS issuer_name, contact_data, connection_status,
             |       pc.node_credential_id, pc.operation_hash, pc.encoded_signed_credential, pc.stored_at,
             |       pc.transaction_id, pc.ledger, pc.shared_at
             |FROM CTE CROSS JOIN credentials c
             |     JOIN PTS USING (issuer_id)
             |     JOIN contacts ON (c.subject_id = contacts.contact_id)
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
             |       external_id, PTS.name AS issuer_name, contact_data, connection_status,
             |       pc.node_credential_id, pc.operation_hash, pc.encoded_signed_credential, pc.stored_at,
             |       pc.transaction_id, pc.ledger, pc.shared_at
             |FROM credentials c
             |     JOIN PTS USING (issuer_id)
             |     JOIN contacts ON (c.subject_id = contacts.contact_id)
             |     LEFT JOIN published_credentials pc USING (credential_id)
             |WHERE c.issuer_id = $issuedBy
             |ORDER BY c.created_on ASC, credential_id
             |LIMIT $limit
             |""".stripMargin
    }
    query.query[GenericCredential].to[List]
  }

  def getBy(issuedBy: Institution.Id, subjectId: Contact.Id): doobie.ConnectionIO[List[GenericCredential]] = {
    sql"""
         |WITH PTS AS (
         |  SELECT id AS issuer_id, name
         |  FROM participants
         |  WHERE tpe = 'issuer'::PARTICIPANT_TYPE
         |)
         |SELECT credential_id, c.issuer_id, c.subject_id, credential_data, group_name, c.created_on,
         |       external_id, PTS.name AS issuer_name, contacts.contact_data, connection_status,
         |       pc.node_credential_id, pc.operation_hash, pc.encoded_signed_credential, pc.stored_at,
         |       pc.transaction_id, pc.ledger, pc.shared_at
         |FROM credentials c
         |     JOIN PTS USING (issuer_id)
         |     JOIN contacts ON (c.subject_id = contacts.contact_id)
         |     LEFT JOIN published_credentials pc USING (credential_id)
         |WHERE c.issuer_id = $issuedBy AND
         |      c.subject_id = $subjectId
         |ORDER BY c.created_on ASC, credential_id
         |""".stripMargin.query[GenericCredential].to[List]
  }

  def storePublicationData(issuerId: Institution.Id, credentialData: PublishCredential): doobie.ConnectionIO[Int] = {
    sql"""
         |INSERT INTO published_credentials (
         |  credential_id, node_credential_id, operation_hash, encoded_signed_credential, transaction_id, ledger, stored_at)
         |SELECT credential_id,
         |       ${credentialData.nodeCredentialId},
         |       ${credentialData.issuanceOperationHash},
         |       ${credentialData.encodedSignedCredential},
         |       ${credentialData.transactionInfo.transactionId},
         |       ${credentialData.transactionInfo.ledger},
         |       now()
         |FROM credentials
         |WHERE credential_id = ${credentialData.credentialId} AND
         |      issuer_id = $issuerId
         |""".stripMargin.update.run.flatTap { n =>
      FC.raiseError(new RuntimeException(s"The credential was not issued by the specified issuer")).whenA(n != 1)
    }
  }

  def markAsShared(institutionId: Institution.Id, credentialId: GenericCredential.Id): doobie.ConnectionIO[Unit] = {
    sql"""
         |UPDATE published_credentials pc
         |SET shared_at = CURRENT_TIMESTAMP
         |FROM credentials c
         |WHERE c.credential_id = pc.credential_id AND
         |      pc.credential_id = $credentialId AND
         |      issuer_id = $institutionId
         |""".stripMargin.update.run
      .flatTap { n =>
        FC.raiseError(new RuntimeException(s"The credential wasn't found or it hasn't been published yet"))
          .whenA(n != 1)
      }
      .map(_ => ())
  }
}
