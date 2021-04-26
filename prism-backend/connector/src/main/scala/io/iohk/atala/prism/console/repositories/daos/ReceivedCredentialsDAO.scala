package io.iohk.atala.prism.console.repositories.daos

import java.util.UUID
import java.time.Instant

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.implicits.legacy.instant._
import cats.syntax.functor._
import io.iohk.atala.prism.connector.model.ConnectionId
import io.iohk.atala.prism.console.models.{Contact, CredentialExternalId, Institution, ReceivedSignedCredential}
import io.iohk.atala.prism.crypto.MerkleTree.MerkleInclusionProof

object ReceivedCredentialsDAO {
  case class StoredReceivedSignedCredentialData(
      connectionId: ConnectionId,
      encodedSignedCredential: String,
      merkleInclusionProof: MerkleInclusionProof,
      credentialExternalId: CredentialExternalId
  )

  def storeReceivedSignedCredential(data: StoredReceivedSignedCredentialData): ConnectionIO[Unit] = {
    val storageId = UUID.randomUUID()
    val storedAt = Instant.now()
    sql"""INSERT INTO stored_credentials (storage_id, connection_id, encoded_signed_credential, inclusion_proof, credential_external_id, stored_at)
         |VALUES ($storageId, ${data.connectionId}, ${data.encodedSignedCredential}, ${data.merkleInclusionProof}, ${data.credentialExternalId}, $storedAt)
         |ON CONFLICT (credential_external_id) DO NOTHING
       """.stripMargin.update.run.void
  }

  def getReceivedCredentialsFor(
      verifierId: Institution.Id,
      maybeContactId: Option[Contact.Id]
  ): ConnectionIO[Seq[ReceivedSignedCredential]] = {
    maybeContactId match {
      case Some(contactId) =>
        sql"""SELECT contact_id, encoded_signed_credential, inclusion_proof, stored_at, external_id
             |FROM stored_credentials JOIN contacts USING (connection_id)
             |WHERE created_by = $verifierId AND contact_id = $contactId
             |ORDER BY stored_at
       """.stripMargin.query[ReceivedSignedCredential].to[Seq]
      case None =>
        sql"""SELECT contact_id, encoded_signed_credential, inclusion_proof, stored_at, external_id
             |FROM stored_credentials JOIN contacts USING (connection_id)
             |WHERE created_by = $verifierId
             |ORDER BY stored_at
       """.stripMargin.query[ReceivedSignedCredential].to[Seq]
    }
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
