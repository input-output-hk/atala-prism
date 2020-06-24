package io.iohk.cvp.cstore.repositories.daos

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import io.iohk.cvp.cstore.models.StoredCredential
import io.iohk.cvp.models.ParticipantId

object StoredCredentialsDAO {
  case class StoredCredentialCreateData(
      individualId: ParticipantId,
      issuerDid: String,
      proofId: String,
      content: Array[Byte],
      signature: Array[Byte]
  )

  def insert(data: StoredCredentialCreateData): ConnectionIO[Unit] = {
    // TODO: should we verify that individualId belongs to the user here?
    sql"""INSERT INTO stored_credentials (individual_id, issuer_did, proof_id, content, signature, stored_at)
         |VALUES (${data.individualId}, ${data.issuerDid}, ${data.proofId},
         |        ${data.content}, ${data.signature}, now())
       """.stripMargin.update.run.map(_ => ())
  }

  def getFor(userId: ParticipantId, individualId: ParticipantId): ConnectionIO[Seq[StoredCredential]] = {
    sql"""SELECT sc.individual_id, sc.issuer_did, sc.proof_id, sc.content, sc.signature
         |FROM stored_credentials sc INNER JOIN verifier_holders vh ON sc.individual_id = vh.holder_id
         |WHERE vh.verifier_id = $userId AND sc.individual_id = $individualId
         |ORDER BY stored_at
       """.stripMargin.query[StoredCredential].to[Seq]
  }
}
