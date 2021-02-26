package io.iohk.atala.prism.kotlin.credentials

import io.iohk.atala.prism.kotlin.crypto.MerkleInclusionProof
import io.iohk.atala.prism.kotlin.crypto.MerkleRoot
import io.iohk.atala.prism.kotlin.crypto.generateProofs
import io.iohk.atala.prism.kotlin.crypto.verifyProof

object CredentialBatches {

    fun batch(
        signedCredentials: List<Credential>
    ): Pair<MerkleRoot, List<MerkleInclusionProof>> {
        return generateProofs(
            signedCredentials.map { it.hash() }
        )
    }

    fun verifyInclusion(
        signedCredential: Credential,
        merkleRoot: MerkleRoot,
        inclusionProof: MerkleInclusionProof
    ): Boolean {
        return signedCredential.hash() == inclusionProof.hash &&
            verifyProof(merkleRoot, inclusionProof)
    }
}
