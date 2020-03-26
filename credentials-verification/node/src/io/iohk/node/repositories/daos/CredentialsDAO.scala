package io.iohk.node.repositories.daos

import java.time.LocalDate

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import io.iohk.cvp.crypto.SHA256Digest
import io.iohk.node.models.{Credential, CredentialId, DIDSuffix}

object CredentialsDAO {
  case class CreateCredentialData(
      id: CredentialId,
      lastOperation: SHA256Digest,
      issuer: DIDSuffix,
      contentHash: SHA256Digest,
      issuedOn: LocalDate
  )

  def insert(
      data: CreateCredentialData
  ): ConnectionIO[Unit] = {
    sql"""
         |INSERT INTO credentials (credential_id, last_operation, issuer, content_hash, issued_on)
         |VALUES (${data.id}, ${data.lastOperation}, ${data.issuer}, ${data.contentHash}, ${data.issuedOn})
       """.stripMargin.update.run.map(_ => ())
  }

  def all(): ConnectionIO[Seq[Credential]] = {
    sql"""
         |SELECT credential_id, issuer, content_hash, issued_on, revoked_on, last_operation
         |FROM credentials
       """.stripMargin.query[Credential].to[Seq]
  }

  def find(credentialId: CredentialId): ConnectionIO[Option[Credential]] = {
    sql"""
         |SELECT credential_id, issuer, content_hash, issued_on, revoked_on, last_operation
         |FROM credentials
         |WHERE credential_id = $credentialId
       """.stripMargin.query[Credential].option
  }

  def revoke(credentialId: CredentialId, revocationDate: LocalDate): ConnectionIO[Boolean] = {
    sql"""
         |UPDATE credentials
         |SET revoked_on = $revocationDate
         |WHERE credential_id = $credentialId
       """.stripMargin.update.run.map(_ > 0)
  }
}
