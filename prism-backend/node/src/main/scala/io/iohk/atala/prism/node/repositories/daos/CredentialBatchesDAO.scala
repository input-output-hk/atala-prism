package io.iohk.atala.prism.node.repositories.daos

import java.time.Instant
import cats.implicits.catsStdInstancesForList
import cats.syntax.functor._
import doobie.{Read, Update}
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.implicits.legacy.instant._
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.kotlin.crypto.MerkleRoot
import io.iohk.atala.prism.kotlin.crypto.SHA256Digest
import io.iohk.atala.prism.identity.DIDSuffix
import io.iohk.atala.prism.models.{Ledger, TransactionId}
import io.iohk.atala.prism.node.models.nodeState.{CredentialBatchState, LedgerData}
import io.iohk.atala.prism.node.repositories.daos._
import io.iohk.atala.prism.interop.implicits._

object CredentialBatchesDAO {
  case class CreateCredentialBatchData(
      batchId: CredentialBatchId,
      lastOperation: SHA256Digest,
      issuerDIDSuffix: DIDSuffix,
      merkleRoot: MerkleRoot,
      ledgerData: LedgerData
  )

  def insert(
      data: CreateCredentialBatchData
  ): ConnectionIO[Unit] = {
    val issuedOn = data.ledgerData.timestampInfo
    sql"""
         |INSERT INTO credential_batches (batch_id, last_operation, issuer_did_suffix, merkle_root, issued_on, issued_on_absn, issued_on_osn, ledger, issued_on_transaction_id)
         |VALUES (${data.batchId}, ${data.lastOperation}, ${data.issuerDIDSuffix}, ${data.merkleRoot.getHash}, ${issuedOn.atalaBlockTimestamp},
         | ${issuedOn.atalaBlockSequenceNumber}, ${issuedOn.operationSequenceNumber}, ${data.ledgerData.ledger}, ${data.ledgerData.transactionId})
       """.stripMargin.update.run.void
  }

  def findBatch(credentialBatchId: CredentialBatchId): ConnectionIO[Option[CredentialBatchState]] = {
    sql"""
         |SELECT batch_id, issuer_did_suffix, merkle_root, issued_on_transaction_id, ledger,
         |       issued_on, issued_on_absn, issued_on_osn, revoked_on_transaction_id, ledger,
         |       revoked_on, revoked_on_absn, revoked_on_osn, last_operation
         |FROM credential_batches
         |WHERE batch_id = $credentialBatchId
       """.stripMargin.query[CredentialBatchState].option
  }

  def revokeEntireBatch(
      credentialBatchId: CredentialBatchId,
      ledgerData: LedgerData
  ): ConnectionIO[Boolean] = {
    val revocationTimestamp = ledgerData.timestampInfo
    sql"""
         |UPDATE credential_batches
         |SET revoked_on = ${revocationTimestamp.atalaBlockTimestamp},
         |    revoked_on_absn = ${revocationTimestamp.atalaBlockSequenceNumber},
         |    revoked_on_osn = ${revocationTimestamp.operationSequenceNumber},
         |    revoked_on_transaction_id = ${ledgerData.transactionId}
         |WHERE batch_id = $credentialBatchId AND
         |      revoked_on IS NULL
       """.stripMargin.update.run.map(_ > 0)
  }

  def revokeCredentials(
      credentialBatchId: CredentialBatchId,
      credentials: List[SHA256Digest],
      ledgerData: LedgerData
  ): ConnectionIO[Unit] = {
    val revocationTimestamp = ledgerData.timestampInfo
    val sql =
      """INSERT INTO revoked_credentials (batch_id, credential_id, revoked_on, revoked_on_absn, revoked_on_osn, ledger, transaction_id)
        |VALUES (?, ?, ?, ?, ?, ?, ?)
        |ON CONFLICT (batch_id, credential_id) DO NOTHING
        |""".stripMargin
    Update[(CredentialBatchId, SHA256Digest, Instant, Int, Int, Ledger, TransactionId)](sql)
      .updateMany(
        credentials.map(credentialHash =>
          (
            credentialBatchId,
            credentialHash,
            revocationTimestamp.atalaBlockTimestamp,
            revocationTimestamp.atalaBlockSequenceNumber,
            revocationTimestamp.operationSequenceNumber,
            ledgerData.ledger,
            ledgerData.transactionId
          )
        )
      )
      .void
  }

  def findRevokedCredentialLedgerData(
      batchId: CredentialBatchId,
      credentialHash: SHA256Digest
  ): ConnectionIO[Option[LedgerData]] = {
    sql"""SELECT transaction_id, ledger, revoked_on, revoked_on_absn, revoked_on_osn
         |FROM revoked_credentials
         |WHERE batch_id = $batchId AND
         |      credential_id = $credentialHash::CREDENTIAL_HASH
         |""".stripMargin.query[LedgerData].option
  }

  // only for testing
  def findRevokedCredentials(batchId: CredentialBatchId): ConnectionIO[List[(SHA256Digest, LedgerData)]] = {
    sql"""SELECT credential_id, transaction_id, ledger, revoked_on, revoked_on_absn, revoked_on_osn
         |FROM revoked_credentials
         |WHERE batch_id = $batchId
         |""".stripMargin.query[(SHA256Digest, LedgerData)].to[List]
  }
}
