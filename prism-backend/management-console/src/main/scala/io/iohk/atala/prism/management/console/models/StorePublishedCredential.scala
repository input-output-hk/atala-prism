package io.iohk.atala.prism.management.console.models

import io.iohk.atala.prism.kotlin.credentials.CredentialBatchId
import io.iohk.atala.prism.kotlin.crypto.MerkleInclusionProof

final case class StorePublishedCredential(
    encodedSignedCredential: String,
    consoleCredentialId: GenericCredential.Id,
    batchId: CredentialBatchId,
    inclusionProof: MerkleInclusionProof
)
