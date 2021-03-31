package io.iohk.atala.prism.console.models.actions

import io.iohk.atala.prism.console.models.GenericCredential.Id
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.MerkleTree.MerkleInclusionProof

case class StorePublishedCredentialRequest(
    encodedSignedCredential: String,
    consoleCredentialId: Id,
    batchId: CredentialBatchId,
    encodedInclusionProof: MerkleInclusionProof
)
