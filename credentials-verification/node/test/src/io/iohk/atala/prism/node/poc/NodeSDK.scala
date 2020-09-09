package io.iohk.atala.prism.node.poc

import com.google.protobuf.ByteString
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.node.models.{CredentialId, DIDSuffix}
import io.iohk.atala.prism.protos.node_models

object NodeSDK {
  def buildIssueCredentialOp(
      credentialHash: SHA256Digest,
      didSuffix: DIDSuffix
  ): node_models.AtalaOperation = {
    node_models.AtalaOperation(
      operation = node_models.AtalaOperation.Operation.IssueCredential(
        node_models.IssueCredentialOperation(
          credentialData = Some(
            node_models.CredentialData(
              issuer = didSuffix.suffix,
              contentHash = ByteString.copyFrom(credentialHash.value)
            )
          )
        )
      )
    )
  }

  def computeCredId(op: node_models.AtalaOperation): CredentialId = {
    CredentialId(SHA256Digest.compute(op.toByteArray))
  }

  def buildRevokeCredentialOp(
      previousOperationHash: SHA256Digest,
      credentialId: CredentialId
  ): node_models.AtalaOperation = {
    node_models.AtalaOperation(
      operation = node_models.AtalaOperation.Operation.RevokeCredential(
        node_models.RevokeCredentialOperation(
          previousOperationHash = ByteString.copyFrom(previousOperationHash.value),
          credentialId = credentialId.id
        )
      )
    )
  }
}
