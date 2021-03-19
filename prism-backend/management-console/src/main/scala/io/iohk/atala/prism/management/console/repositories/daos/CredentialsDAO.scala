package io.iohk.atala.prism.management.console.repositories.daos

import cats.data.NonEmptyList

import java.time.Instant

import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.implicits.legacy.instant._
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.models.TransactionInfo

object CredentialsDAO {

  private val withParticipantsPTS =
    fr"""
        |PTS AS (
        |   SELECT participant_id AS issuer_id, name
        |   FROM participants
        |)""".stripMargin

  private val selectGenericCredential =
    fr"""
        |SELECT credential_id, c.issuer_id, c.contact_id, credential_data, c.created_on, c.credential_type_id,
        |       c.credential_issuance_contact_id, external_id, PTS.name AS issuer_name, contact_data,
        |       pc.node_credential_id, pc.operation_hash, pc.encoded_signed_credential, pc.stored_at,
        |       pc.transaction_id, pc.ledger, pc.shared_at
      """.stripMargin

  def create(
      participantId: ParticipantId,
      contactId: Contact.Id,
      data: CreateGenericCredential
  ): doobie.ConnectionIO[GenericCredential] = {
    val id = GenericCredential.Id.random()
    val createdOn = Instant.now()
    (fr"""
         |WITH inserted AS (
         |  INSERT INTO draft_credentials (credential_id, issuer_id, contact_id, credential_data,
         |    created_on, credential_issuance_contact_id, credential_type_id)
         |  VALUES ($id, $participantId, $contactId, ${data.credentialData},
         |    $createdOn, ${data.credentialIssuanceContactId}, ${data.credentialTypeId})
         |  RETURNING credential_id, issuer_id, contact_id, credential_data, created_on, credential_type_id,
         |    credential_issuance_contact_id
         |),""".stripMargin ++ withParticipantsPTS ++
      fr"""|SELECT inserted.*, contacts.external_id, PTS.name AS issuer_name, contacts.contact_data,
         |       pc.node_credential_id, pc.operation_hash, pc.encoded_signed_credential, pc.stored_at,
         |       pc.transaction_id, pc.ledger, pc.shared_at
         |FROM inserted
         |     JOIN PTS USING (issuer_id)
         |     JOIN contacts ON (inserted.contact_id = contacts.contact_id)
         |     LEFT JOIN published_credentials pc USING (credential_id)
         |""".stripMargin)
      .query[GenericCredential]
      .unique
  }

  def getBy(credentialId: GenericCredential.Id): doobie.ConnectionIO[Option[GenericCredential]] = {
    (fr"WITH" ++ withParticipantsPTS ++ selectGenericCredential ++
      fr"""
         |FROM draft_credentials c
         |     JOIN PTS USING (issuer_id)
         |     JOIN contacts ON (c.contact_id = contacts.contact_id)
         |     LEFT JOIN published_credentials pc USING (credential_id)
         |WHERE credential_id = $credentialId
         |""".stripMargin)
      .query[GenericCredential]
      .option
  }

  def getBy(
      issuedBy: ParticipantId,
      limit: Int,
      lastSeenCredential: Option[GenericCredential.Id]
  ): doobie.ConnectionIO[List[GenericCredential]] = {
    val query = lastSeenCredential match {
      case Some(lastSeen) =>
        fr"""
             |WITH CTE AS (
             |  SELECT created_on AS last_seen_time
             |  FROM draft_credentials
             |  WHERE credential_id = $lastSeen
             |),""".stripMargin ++ withParticipantsPTS ++ selectGenericCredential ++
          fr"""
             |FROM CTE CROSS JOIN draft_credentials c
             |     JOIN PTS USING (issuer_id)
             |     JOIN contacts ON (c.contact_id = contacts.contact_id)
             |     LEFT JOIN published_credentials pc USING (credential_id)
             |WHERE c.issuer_id = $issuedBy AND
             |      (c.created_on > last_seen_time OR (c.created_on = last_seen_time AND credential_id > $lastSeen))
             |ORDER BY c.created_on ASC, credential_id
             |LIMIT $limit
             |""".stripMargin
      case None =>
        fr"WITH" ++ withParticipantsPTS ++ selectGenericCredential ++
          fr"""
             |FROM draft_credentials c
             |     JOIN PTS USING (issuer_id)
             |     JOIN contacts ON (c.contact_id = contacts.contact_id)
             |     LEFT JOIN published_credentials pc USING (credential_id)
             |WHERE c.issuer_id = $issuedBy
             |ORDER BY c.created_on ASC, credential_id
             |LIMIT $limit
             |""".stripMargin
    }
    query.query[GenericCredential].to[List]
  }

  def getBy(issuedBy: ParticipantId, subjectId: Contact.Id): doobie.ConnectionIO[List[GenericCredential]] = {
    (fr"WITH" ++ withParticipantsPTS ++ selectGenericCredential ++
      fr"""
         |FROM draft_credentials c
         |     JOIN PTS USING (issuer_id)
         |     JOIN contacts ON (c.contact_id = contacts.contact_id)
         |     LEFT JOIN published_credentials pc USING (credential_id)
         |WHERE c.issuer_id = $issuedBy AND
         |      c.contact_id = $subjectId
         |ORDER BY c.created_on ASC, credential_id
         |""".stripMargin)
      .query[GenericCredential]
      .to[List]
  }

  def getIssuedCredentialsBy(
      issuedBy: ParticipantId,
      subjectId: Contact.Id
  ): doobie.ConnectionIO[List[GenericCredential]] = {
    (fr"WITH" ++ withParticipantsPTS ++ selectGenericCredential ++
      fr"""
          |FROM draft_credentials c
          |     JOIN PTS USING (issuer_id)
          |     JOIN contacts ON (c.contact_id = contacts.contact_id)
          |     JOIN published_credentials pc USING (credential_id)
          |WHERE c.issuer_id = $issuedBy AND
          |      c.contact_id = $subjectId
          |ORDER BY c.created_on ASC, credential_id
          |""".stripMargin)
      .query[GenericCredential]
      .to[List]
  }

  def storePublicationData(issuerId: ParticipantId, credentialData: PublishCredential): doobie.ConnectionIO[Int] = {
    sql"""
         |INSERT INTO published_credentials (
         |  credential_id, node_credential_id, operation_hash, encoded_signed_credential, transaction_id, ledger, stored_at)
         |SELECT credential_id,
         |       ${credentialData.nodeCredentialId},
         |       ${credentialData.issuanceOperationHash},
         |       ${credentialData.encodedSignedCredential},
         |       ${credentialData.transactionInfo.transactionId},
         |       ${credentialData.transactionInfo.ledger},
         |       ${Instant.now()}
         |FROM draft_credentials
         |WHERE credential_id = ${credentialData.credentialId} AND
         |      issuer_id = $issuerId
         |""".stripMargin.update.run.flatTap { n =>
      FC.raiseError(new RuntimeException(s"The credential was not issued by the specified issuer")).whenA(n != 1)
    }
  }

  def storeBatchData(
      batchId: CredentialBatchId,
      issuanceTransactionInfo: TransactionInfo,
      issuanceOperationHash: SHA256Digest
  ): doobie.ConnectionIO[Int] = {
    sql"""
         |INSERT INTO published_batches (
         |  batch_id, issued_on_transaction_id, ledger, issuance_operation_hash, stored_at
         |)
         |VALUES ($batchId,
         |        ${issuanceTransactionInfo.transactionId},
         |        ${issuanceTransactionInfo.ledger},
         |        ${issuanceOperationHash},
         |        ${Instant.now()}
         |)
         |""".stripMargin.update.run
  }

  def markAsShared(
      institutionId: ParticipantId,
      credentialsIds: NonEmptyList[GenericCredential.Id]
  ): doobie.ConnectionIO[Unit] = {
    (fr"""
         |UPDATE published_credentials pc
         |SET shared_at = CURRENT_TIMESTAMP
         |FROM draft_credentials c
         |WHERE c.credential_id = pc.credential_id AND
         |      issuer_id = $institutionId AND""".stripMargin ++
      Fragments.in(fr"pc.credential_id", credentialsIds)).update.run.flatTap { n =>
      FC.raiseError(
          new RuntimeException(s"Cannot mark credentials as shared. Updated rows: $n expected ${credentialsIds.size}")
        )
        .whenA(n != credentialsIds.size)
    }.void
  }

  def verifyPublishedCredentialsExist(
      institutionId: ParticipantId,
      credentialsIds: NonEmptyList[GenericCredential.Id]
  ): doobie.ConnectionIO[List[GenericCredential.Id]] = {
    (fr"""
         |SELECT pc.credential_id
         |FROM published_credentials pc
         |JOIN draft_credentials c USING(credential_id)
         |WHERE c.issuer_id = $institutionId AND""".stripMargin ++
      Fragments.in(fr"pc.credential_id", credentialsIds))
      .query[GenericCredential.Id]
      .to[List]
  }

  def deleteBy(contactId: Contact.Id): doobie.ConnectionIO[Int] = {
    sql"""
         |DELETE FROM draft_credentials
         |WHERE contact_id = $contactId
         |""".stripMargin.update.run
  }

  def deletePublishedCredentialsBy(contactId: Contact.Id): doobie.ConnectionIO[Int] = {
    sql"""
         |DELETE FROM published_credentials
         |USING draft_credentials
         |WHERE published_credentials.credential_id = draft_credentials.credential_id AND
         |      draft_credentials.contact_id = $contactId
         |""".stripMargin.update.run
  }
}
