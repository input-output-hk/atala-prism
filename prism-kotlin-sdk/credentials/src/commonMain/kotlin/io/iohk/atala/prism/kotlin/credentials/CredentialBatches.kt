package io.iohk.atala.prism.kotlin.credentials

import io.iohk.atala.prism.kotlin.crypto.*
import io.iohk.atala.prism.kotlin.identity.DID
import io.iohk.atala.prism.kotlin.protos.CredentialBatchData
import pbandk.ByteArr
import pbandk.encodeToByteArray

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

    fun computeCredentialBatchId(did: DID, merkleRoot: MerkleRoot): CredentialBatchId {
        val data = CredentialBatchData(did.suffix.value, ByteArr(merkleRoot.hash.value))
        return CredentialBatchId.fromDigest(SHA256Digest.compute(data.encodeToByteArray()))
    }
}
