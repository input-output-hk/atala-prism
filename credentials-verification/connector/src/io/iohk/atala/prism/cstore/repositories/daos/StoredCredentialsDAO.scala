package io.iohk.atala.prism.cstore.repositories.daos

import java.util.UUID

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import io.iohk.atala.prism.connector.model.ConnectionId
import io.iohk.atala.prism.cstore.models.StoredSignedCredential
import io.iohk.atala.prism.models.ParticipantId

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
      verifierId: ParticipantId,
      individualId: ParticipantId
  ): ConnectionIO[Seq[StoredSignedCredential]] = {
    sql"""SELECT contact_id, encoded_signed_credential, stored_at
         |FROM stored_credentials JOIN contacts USING (connection_id)
         |WHERE created_by = $verifierId AND contact_id = $individualId
         |ORDER BY stored_at
       """.stripMargin.query[StoredSignedCredential].to[Seq]
  }
}
