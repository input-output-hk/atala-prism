package io.iohk.atala.prism.node.repositories.daos

import java.time.Instant

import cats.implicits.catsStdInstancesForList
import doobie.Update
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.implicits.legacy.instant._
import io.iohk.atala.prism.credentials.{CredentialBatchId, TimestampInfo}
import io.iohk.atala.prism.crypto.MerkleTree.MerkleRoot
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.node.models.DIDSuffix
import io.iohk.atala.prism.node.models.nodeState.CredentialBatchState

object CredentialBatchesDAO {
  case class CreateCredentialBatchData(
      batchId: CredentialBatchId,
      lastOperation: SHA256Digest,
      issuerDIDSuffix: DIDSuffix,
      merkleRoot: MerkleRoot,
      issuedOn: TimestampInfo
  )

  def insert(
      data: CreateCredentialBatchData
  ): ConnectionIO[Unit] = {
    sql"""
         |INSERT INTO credential_batches (batch_id, last_operation, issuer_did_suffix, merkle_root, issued_on, issued_on_absn, issued_on_osn)
         |VALUES (${data.batchId}, ${data.lastOperation}, ${data.issuerDIDSuffix}, ${data.merkleRoot}, ${data.issuedOn.atalaBlockTimestamp}, ${data.issuedOn.atalaBlockSequenceNumber}, ${data.issuedOn.operationSequenceNumber})
       """.stripMargin.update.run.map(_ => ())
  }

  def findBatch(credentialBatchId: CredentialBatchId): ConnectionIO[Option[CredentialBatchState]] = {
    sql"""
         |SELECT batch_id, issuer_did_suffix, merkle_root, issued_on, issued_on_absn, issued_on_osn, revoked_on, revoked_on_absn, revoked_on_osn, last_operation
         |FROM credential_batches
         |WHERE batch_id = $credentialBatchId
       """.stripMargin.query[CredentialBatchState].option
  }

  def revokeEntireBatch(
      credentialBatchId: CredentialBatchId,
      revocationTimestamp: TimestampInfo
  ): ConnectionIO[Boolean] = {
    sql"""
         |UPDATE credential_batches
         |SET revoked_on = ${revocationTimestamp.atalaBlockTimestamp}, revoked_on_absn = ${revocationTimestamp.atalaBlockSequenceNumber}, revoked_on_osn = ${revocationTimestamp.operationSequenceNumber}
         |WHERE batch_id = $credentialBatchId AND
         |      revoked_on IS NULL
       """.stripMargin.update.run.map(_ > 0)
  }

  def revokeCredentials(
      credentialBatchId: CredentialBatchId,
      credentials: List[SHA256Digest],
      revocationTimestamp: TimestampInfo
  ): ConnectionIO[Unit] = {
    val sql =
      """INSERT INTO revoked_credentials (batch_id, credential_id, revoked_on, revoked_on_absn, revoked_on_osn)
        |VALUES (?, ?, ?, ?, ?)
        |ON CONFLICT (batch_id, credential_id) DO NOTHING
        |""".stripMargin
    Update[(CredentialBatchId, SHA256Digest, Instant, Int, Int)](sql)
      .updateMany(
        credentials.map(credentialHash =>
          (
            credentialBatchId,
            credentialHash,
            revocationTimestamp.atalaBlockTimestamp,
            revocationTimestamp.atalaBlockSequenceNumber,
            revocationTimestamp.operationSequenceNumber
          )
        )
      )
      .map(_ => ())
  }

  def findRevokedCredentialTime(
      batchId: CredentialBatchId,
      credentialHash: SHA256Digest
  ): ConnectionIO[Option[TimestampInfo]] = {
    sql"""SELECT revoked_on, revoked_on_absn, revoked_on_osn
         |FROM revoked_credentials
         |WHERE batch_id = $batchId AND
         |      credential_id = $credentialHash::CREDENTIAL_HASH
         |""".stripMargin.query[TimestampInfo].option
  }

  // only for testing
  def findRevokedCredentials(batchId: CredentialBatchId): ConnectionIO[List[(SHA256Digest, TimestampInfo)]] = {
    sql"""SELECT credential_id, revoked_on, revoked_on_absn, revoked_on_osn
         |FROM revoked_credentials
         |WHERE batch_id = $batchId
         |""".stripMargin.query[(SHA256Digest, TimestampInfo)].to[List]
  }
}
