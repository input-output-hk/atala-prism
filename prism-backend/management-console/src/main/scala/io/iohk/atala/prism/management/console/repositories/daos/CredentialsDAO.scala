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
import io.iohk.atala.prism.models.{TransactionId, TransactionInfo}

object CredentialsDAO {

  private val withParticipantsPTS =
    fr"""
        |PTS AS (
        |   SELECT participant_id AS issuer_id, name
        |   FROM participants
        |),""".stripMargin

  private val selectGenericCredential =
    fr"""
        |SELECT credential_id, c.issuer_id, c.contact_id, credential_data, c.created_on, c.credential_type_id,
        |       c.credential_issuance_contact_id, external_id, PTS.name AS issuer_name, contact_data,
        |       PC.batch_id, PC.issuance_operation_hash, PC.encoded_signed_credential, PC.inclusion_proof,
        |       PC.stored_at, PC.issued_on_transaction_id, PC.ledger, PC.shared_at, PC.revoked_on_transaction_id
      """.stripMargin

  private def withPublishedCredentialsPC(maybeCredentialId: Option[GenericCredential.Id] = None) =
    maybeCredentialId match {
      case Some(credentialId) =>
        fr"""
           |PC AS (
           |  SELECT credential_id, batch_id, issuance_operation_hash, encoded_signed_credential, inclusion_proof,
           |         stored_at, issued_on_transaction_id, ledger, shared_at, revoked_on_transaction_id
           |  FROM published_credentials JOIN published_batches USING (batch_id)
           |  WHERE credential_id = $credentialId
           |)
          """.stripMargin
      case None =>
        fr"""
          |PC AS (
          |  SELECT credential_id, batch_id, issuance_operation_hash, encoded_signed_credential, inclusion_proof,
          |         stored_at, issued_on_transaction_id, ledger, shared_at, revoked_on_transaction_id
          |  FROM published_credentials JOIN published_batches USING (batch_id)
          |)
          """.stripMargin
    }

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
         |),""".stripMargin ++ withParticipantsPTS ++ withPublishedCredentialsPC(Some(id)) ++
      fr"""|SELECT inserted.*, contacts.external_id, PTS.name AS issuer_name, contacts.contact_data,
           |       PC.batch_id, PC.issuance_operation_hash, PC.encoded_signed_credential, PC.inclusion_proof,
           |       PC.stored_at, PC.issued_on_transaction_id, PC.ledger, PC.shared_at, PC.revoked_on_transaction_id
         |FROM inserted
         |     JOIN PTS USING (issuer_id)
         |     JOIN contacts ON (inserted.contact_id = contacts.contact_id)
         |     LEFT JOIN PC USING (credential_id)
         |""".stripMargin)
      .query[GenericCredential]
      .unique
  }

  def getBy(credentialId: GenericCredential.Id): doobie.ConnectionIO[Option[GenericCredential]] = {
    (fr"WITH" ++ withParticipantsPTS ++ withPublishedCredentialsPC(Some(credentialId)) ++ selectGenericCredential ++
      fr"""
         |FROM draft_credentials c
         |     JOIN PTS USING (issuer_id)
         |     JOIN contacts ON (c.contact_id = contacts.contact_id)
         |     LEFT JOIN PC USING (credential_id)
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
             |),""".stripMargin ++ withParticipantsPTS ++ withPublishedCredentialsPC() ++ selectGenericCredential ++
          fr"""
             |FROM CTE CROSS JOIN draft_credentials c
             |     JOIN PTS USING (issuer_id)
             |     JOIN contacts ON (c.contact_id = contacts.contact_id)
             |     LEFT JOIN PC USING (credential_id)
             |WHERE c.issuer_id = $issuedBy AND
             |      (c.created_on > last_seen_time OR (c.created_on = last_seen_time AND credential_id > $lastSeen))
             |ORDER BY c.created_on ASC, credential_id
             |LIMIT $limit
             |""".stripMargin
      case None =>
        fr"WITH" ++ withParticipantsPTS ++ withPublishedCredentialsPC() ++ selectGenericCredential ++
          fr"""
             |FROM draft_credentials c
             |     JOIN PTS USING (issuer_id)
             |     JOIN contacts ON (c.contact_id = contacts.contact_id)
             |     LEFT JOIN PC USING (credential_id)
             |WHERE c.issuer_id = $issuedBy
             |ORDER BY c.created_on ASC, credential_id
             |LIMIT $limit
             |""".stripMargin
    }
    query.query[GenericCredential].to[List]
  }

  def getBy(issuedBy: ParticipantId, contactId: Contact.Id): doobie.ConnectionIO[List[GenericCredential]] = {
    (fr"WITH" ++ withParticipantsPTS ++ withPublishedCredentialsPC() ++ selectGenericCredential ++
      fr"""
         |FROM draft_credentials c
         |     JOIN PTS USING (issuer_id)
         |     JOIN contacts ON (c.contact_id = contacts.contact_id)
         |     LEFT JOIN PC USING (credential_id)
         |WHERE c.issuer_id = $issuedBy AND
         |      c.contact_id = $contactId
         |ORDER BY c.created_on ASC, credential_id
         |""".stripMargin)
      .query[GenericCredential]
      .to[List]
  }

  def getIssuedCredentialsBy(
      issuedBy: ParticipantId,
      contactId: Contact.Id
  ): doobie.ConnectionIO[List[GenericCredential]] = {
    (fr"WITH" ++ withParticipantsPTS ++ withPublishedCredentialsPC() ++ selectGenericCredential ++
      fr"""
          |FROM draft_credentials c
          |     JOIN PTS USING (issuer_id)
          |     JOIN contacts ON (c.contact_id = contacts.contact_id)
          |     JOIN PC USING (credential_id)
          |WHERE c.issuer_id = $issuedBy AND
          |      c.contact_id = $contactId
          |ORDER BY c.created_on ASC, credential_id
          |""".stripMargin)
      .query[GenericCredential]
      .to[List]
  }

  def storePublicationData(issuerId: ParticipantId, credentialData: PublishCredential): doobie.ConnectionIO[Int] = {
    sql"""
         |INSERT INTO published_credentials (
         |  credential_id, batch_id, encoded_signed_credential, inclusion_proof
         |)
         |SELECT credential_id,
         |       ${credentialData.credentialBatchId},
         |       ${credentialData.encodedSignedCredential},
         |       ${credentialData.proofOfInclusion}
         |FROM draft_credentials
         |WHERE credential_id = ${credentialData.consoleCredentialId} AND
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

  def revokeCredential(
      institutionId: ParticipantId,
      credentialId: GenericCredential.Id,
      transactionId: TransactionId
  ): doobie.ConnectionIO[Unit] = {
    sql"""
         |WITH institution_credentials AS (
         |  SELECT credential_id
         |  FROM draft_credentials
         |  WHERE credential_id = $credentialId AND
         |        issuer_id = $institutionId
         |)
         |UPDATE published_credentials
         |SET revoked_on_transaction_id = $transactionId
         |WHERE credential_id = $credentialId AND
         |      credential_id IN (SELECT * FROM institution_credentials)
         |""".stripMargin.update.run.flatTap { n =>
      FC.raiseError(new RuntimeException(s"The credential wasn't found or it hasn't been published yet"))
        .whenA(n != 1)
    }.void
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

  def getIdsOfPublishedNotRevokedCredentials(
      institutionId: ParticipantId,
      credentialsIds: NonEmptyList[GenericCredential.Id]
  ): doobie.ConnectionIO[List[GenericCredential.Id]] = {
    (fr"""
         |SELECT pc.credential_id
         |FROM published_credentials pc
         |JOIN draft_credentials c USING(credential_id)
         |WHERE c.issuer_id = $institutionId AND pc.revoked_on_transaction_id IS NULL AND""".stripMargin ++
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

  def deleteBy(
      institutionId: ParticipantId,
      credentialsIds: NonEmptyList[GenericCredential.Id]
  ): doobie.ConnectionIO[Unit] = {
    (fr"""
         |DELETE FROM draft_credentials
         |WHERE issuer_id = $institutionId AND
         |""".stripMargin ++
      Fragments.in(fr"credential_id", credentialsIds)).update.run.flatTap { n =>
      FC.raiseError(
          new RuntimeException(s"Cannot delete credentials. Updated rows: $n expected ${credentialsIds.size}")
        )
        .whenA(n != credentialsIds.size)
    }.void
  }

  def deletePublishedCredentialsBy(
      institutionId: ParticipantId,
      credentialsIds: NonEmptyList[GenericCredential.Id]
  ): doobie.ConnectionIO[Unit] = {
    (fr"""
         |DELETE FROM published_credentials pc
         |USING draft_credentials dc
         |WHERE pc.credential_id = dc.credential_id AND
         |      dc.issuer_id = ${institutionId} AND
         |""".stripMargin ++
      Fragments.in(fr"pc.credential_id", credentialsIds)).update.run.void
  }
}
