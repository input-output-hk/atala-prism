package io.iohk.atala.prism.console.repositories.daos

import java.util.UUID

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import io.iohk.atala.prism.connector.model.ConnectionId
import io.iohk.atala.prism.console.models.{Contact, Institution, StoredSignedCredential}

object StoredCredentialsDAO {
  case class StoredSignedCredentialData(
      connectionId: ConnectionId,
      encodedSignedCredential: String
  )

  def storeSignedCredential(data: StoredSignedCredentialData): ConnectionIO[Unit] = {
    val storageId = UUID.randomUUID()
    sql"""INSERT INTO stored_credentials (storage_id, connection_id, encoded_signed_credential, stored_at)
         |VALUES ($storageId, ${data.connectionId}, ${data.encodedSignedCredential}, now())
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
}
