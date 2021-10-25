package io.iohk.atala.prism.utils

import com.google.protobuf.ByteString
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.{MerkleRoot, Sha256Digest}
import io.iohk.atala.prism.protos.node_models
import io.iohk.atala.prism.identity.{PrismDid => DID}

object NodeClientUtils {

  def issueBatchOperation(
      issuerDID: DID,
      merkleRoot: MerkleRoot
  ): node_models.AtalaOperation = {
    node_models
      .AtalaOperation(
        operation = node_models.AtalaOperation.Operation.IssueCredentialBatch(
          value = node_models
            .IssueCredentialBatchOperation(
              credentialBatchData = Some(
                node_models.CredentialBatchData(
                  issuerDid = issuerDID.getSuffix,
                  merkleRoot = toByteString(merkleRoot.getHash)
                )
              )
            )
        )
      )
  }

  def revokeCredentialsOperation(
      previousOperationHash: Sha256Digest,
      batchId: CredentialBatchId,
      credentialsToRevoke: Seq[Sha256Digest] = Nil
  ): node_models.AtalaOperation = {
    node_models
      .AtalaOperation(
        operation = node_models.AtalaOperation.Operation.RevokeCredentials(
          value = node_models
            .RevokeCredentialsOperation(
              previousOperationHash = toByteString(previousOperationHash),
              credentialBatchId = batchId.getId,
              credentialsToRevoke = credentialsToRevoke.map(toByteString)
            )
        )
      )
  }

  def toByteString(hash: Sha256Digest): ByteString =
    ByteString.copyFrom(hash.getValue)

}
