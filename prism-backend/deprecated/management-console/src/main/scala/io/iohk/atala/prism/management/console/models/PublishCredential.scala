package io.iohk.atala.prism.management.console.models

import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.MerkleInclusionProof

case class PublishCredential(
    consoleCredentialId: GenericCredential.Id,
    credentialBatchId: CredentialBatchId,
    encodedSignedCredential: String,
    proofOfInclusion: MerkleInclusionProof
)
