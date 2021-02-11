package io.iohk.atala.prism.kotlin.credentials

import io.iohk.atala.prism.kotlin.crypto.MerkleTree

object CredentialBatches {

    fun batch(
        signedCredentials: List<Credential>
    ): Pair<MerkleTree.MerkleRoot, List<MerkleTree.MerkleInclusionProof>> {
        return MerkleTree.generateProofs(
            signedCredentials.map { it.hash() }
        )
    }

    fun verifyInclusion(
        signedCredential: Credential,
        merkleRoot: MerkleTree.MerkleRoot,
        inclusionProof: MerkleTree.MerkleInclusionProof
    ): Boolean {
        return signedCredential.hash() == inclusionProof.hash &&
            MerkleTree.verifyProof(merkleRoot, inclusionProof)
    }
}
