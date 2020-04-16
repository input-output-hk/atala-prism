package io.iohk.node.repositories.daos

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import io.iohk.cvp.crypto.SHA256Digest
import io.iohk.node.models.nodeState.CredentialState
import io.iohk.node.models.{CredentialId, DIDSuffix}
import io.iohk.node.operations.TimestampInfo

object CredentialsDAO {
  case class CreateCredentialData(
      id: CredentialId,
      lastOperation: SHA256Digest,
      issuer: DIDSuffix,
      contentHash: SHA256Digest,
      timestampInfo: TimestampInfo
  )

  def insert(
      data: CreateCredentialData
  ): ConnectionIO[Unit] = {
    sql"""
         |INSERT INTO credentials (credential_id, last_operation, issuer, content_hash, issued_on, issued_on_absn, issued_on_osn)
         |VALUES (${data.id}, ${data.lastOperation}, ${data.issuer}, ${data.contentHash}, ${data.timestampInfo.atalaBlockTimestamp}, ${data.timestampInfo.atalaBlockSequenceNumber}, ${data.timestampInfo.operationSequenceNumber})
       """.stripMargin.update.run.map(_ => ())
  }

  def all(): ConnectionIO[Seq[CredentialState]] = {
    sql"""
         |SELECT credential_id, issuer, content_hash, issued_on, issued_on_absn, issued_on_osn, revoked_on, revoked_on_absn, revoked_on_osn, last_operation
         |FROM credentials
       """.stripMargin.query[CredentialState].to[Seq]
  }

  def find(credentialId: CredentialId): ConnectionIO[Option[CredentialState]] = {
    sql"""
         |SELECT credential_id, issuer, content_hash, issued_on, issued_on_absn, issued_on_osn, revoked_on, revoked_on_absn, revoked_on_osn, last_operation
         |FROM credentials
         |WHERE credential_id = $credentialId
       """.stripMargin.query[CredentialState].option
  }

  def revoke(credentialId: CredentialId, revocationTimestamp: TimestampInfo): ConnectionIO[Boolean] = {
    sql"""
         |UPDATE credentials
         |SET revoked_on = ${revocationTimestamp.atalaBlockTimestamp}, revoked_on_absn = ${revocationTimestamp.atalaBlockSequenceNumber}, revoked_on_osn = ${revocationTimestamp.operationSequenceNumber}
         |WHERE credential_id = $credentialId
       """.stripMargin.update.run.map(_ > 0)
  }
}
