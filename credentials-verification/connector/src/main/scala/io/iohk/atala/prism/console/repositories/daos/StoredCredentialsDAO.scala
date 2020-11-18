package io.iohk.atala.prism.console.repositories.daos

import java.util.UUID

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.implicits.legacy.instant._
import io.iohk.atala.prism.connector.model.ConnectionId
import io.iohk.atala.prism.console.models.{Contact, CredentialExternalId, Institution, StoredSignedCredential}

object StoredCredentialsDAO {
  case class StoredSignedCredentialData(
      connectionId: ConnectionId,
      encodedSignedCredential: String,
      credentialExternalId: CredentialExternalId
  )

  def storeSignedCredential(data: StoredSignedCredentialData): ConnectionIO[Unit] = {
    val storageId = UUID.randomUUID()
    sql"""INSERT INTO stored_credentials (storage_id, connection_id, encoded_signed_credential, credential_external_id, stored_at)
         |VALUES ($storageId, ${data.connectionId}, ${data.encodedSignedCredential}, ${data.credentialExternalId}, now())
         |ON CONFLICT (credential_external_id) DO NOTHING
       """.stripMargin.update.run.map(_ => ())
  }

  def getStoredCredentialsFor(
      verifierId: Institution.Id,
      contactId: Contact.Id
  ): ConnectionIO[Seq[StoredSignedCredential]] = {
    sql"""SELECT contact_id, encoded_signed_credential, stored_at
         |FROM stored_credentials JOIN contacts USING (connection_id)
         |WHERE created_by = $verifierId AND contact_id = $contactId
         |ORDER BY stored_at
       """.stripMargin.query[StoredSignedCredential].to[Seq]
  }

  def getLatestCredentialExternalId(
      verifierId: Institution.Id
  ): ConnectionIO[Option[CredentialExternalId]] = {
    sql"""SELECT credential_external_id
         |FROM stored_credentials JOIN contacts USING (connection_id)
         |WHERE created_by = $verifierId
         |ORDER BY stored_at DESC
         |LIMIT 1
       """.stripMargin.query[CredentialExternalId].option
  }
}
