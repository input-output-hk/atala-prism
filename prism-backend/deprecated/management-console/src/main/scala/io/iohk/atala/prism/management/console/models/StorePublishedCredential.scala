package io.iohk.atala.prism.management.console.models

import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.MerkleInclusionProof

final case class StorePublishedCredential(
    encodedSignedCredential: String,
    consoleCredentialId: GenericCredential.Id,
    batchId: CredentialBatchId,
    inclusionProof: MerkleInclusionProof
)
