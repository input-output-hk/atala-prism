package io.iohk.atala.prism.management.console.repositories.daos

import java.time.Instant
import java.util.UUID

import cats.syntax.functor._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.implicits.legacy.instant._
import io.iohk.atala.prism.management.console.models.{
  Contact,
  CredentialExternalId,
  ParticipantId,
  ReceivedSignedCredential
}

object ReceivedCredentialsDAO {
  case class ReceivedSignedCredentialData(
      contactId: Contact.Id,
      encodedSignedCredential: String,
      credentialExternalId: CredentialExternalId
  )

  def insertSignedCredential(
      data: ReceivedSignedCredentialData
  ): ConnectionIO[Unit] = {
    val receivedId = UUID.randomUUID()
    val receivedAt = Instant.now()
    sql"""INSERT INTO received_credentials (received_id, contact_id, encoded_signed_credential, credential_external_id, received_at)
         |VALUES ($receivedId, ${data.contactId}, ${data.encodedSignedCredential}, ${data.credentialExternalId}, $receivedAt)
         |ON CONFLICT (credential_external_id) DO NOTHING
       """.stripMargin.update.run.void
  }

  def getReceivedCredentialsFor(
      verifierId: ParticipantId,
      contactIdMaybe: Option[Contact.Id]
  ): ConnectionIO[List[ReceivedSignedCredential]] = {
    val statement = contactIdMaybe match {
      case Some(contactId) =>
        sql"""SELECT contact_id, encoded_signed_credential, received_at
             |FROM received_credentials JOIN contacts USING (contact_id)
             |WHERE created_by = $verifierId AND contact_id = $contactId
             |ORDER BY received_at
       """.stripMargin

      case None =>
        sql"""SELECT contact_id, encoded_signed_credential, received_at
             |FROM received_credentials JOIN contacts USING (contact_id)
             |WHERE created_by = $verifierId
             |ORDER BY received_at
       """.stripMargin
    }

    statement.query[ReceivedSignedCredential].to[List]
  }

  def getLatestCredentialExternalId(
      verifierId: ParticipantId
  ): ConnectionIO[Option[CredentialExternalId]] = {
    sql"""SELECT credential_external_id
         |FROM received_credentials JOIN contacts USING (contact_id)
         |WHERE created_by = $verifierId
         |ORDER BY received_at DESC
         |LIMIT 1
       """.stripMargin.query[CredentialExternalId].option
  }
}
