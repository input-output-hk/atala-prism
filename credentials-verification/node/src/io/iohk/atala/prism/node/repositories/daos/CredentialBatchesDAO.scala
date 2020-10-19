package io.iohk.atala.prism.node.repositories.daos

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.MerkleTree.MerkleRoot
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.node.models.DIDSuffix
import io.iohk.atala.prism.node.models.nodeState.CredentialBatchState
import io.iohk.atala.prism.node.operations.TimestampInfo

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

  def find(credentialBatchId: CredentialBatchId): ConnectionIO[Option[CredentialBatchState]] = {
    sql"""
         |SELECT batch_id, issuer_did_suffix, merkle_root, issued_on, issued_on_absn, issued_on_osn, revoked_on, revoked_on_absn, revoked_on_osn, last_operation
         |FROM credential_batches
         |WHERE batch_id = $credentialBatchId
       """.stripMargin.query[CredentialBatchState].option
  }
}
