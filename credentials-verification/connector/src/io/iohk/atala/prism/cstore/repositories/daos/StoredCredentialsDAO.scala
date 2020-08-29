package io.iohk.atala.prism.cstore.repositories.daos

import java.util.UUID

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import io.iohk.connector.model.ConnectionId
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
    sql"""SELECT vh.holder_id, encoded_signed_credential, stored_at
         |FROM stored_credentials sc INNER JOIN verifier_holders vh ON sc.connection_id = vh.connection_id
         |WHERE vh.verifier_id = $verifierId AND vh.holder_id = $individualId
         |ORDER BY stored_at
       """.stripMargin.query[StoredSignedCredential].to[Seq]
  }
}
