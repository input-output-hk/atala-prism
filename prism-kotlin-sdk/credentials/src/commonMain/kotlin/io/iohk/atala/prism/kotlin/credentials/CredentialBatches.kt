package io.iohk.atala.prism.kotlin.credentials

import io.iohk.atala.prism.kotlin.crypto.*
import io.iohk.atala.prism.kotlin.identity.DID
import io.iohk.atala.prism.kotlin.protos.CredentialBatchData
import pbandk.ByteArr
import pbandk.encodeToByteArray
import kotlin.js.JsExport
import kotlin.jvm.JvmStatic

@JsExport
data class CredentialBatch(
    val root: MerkleRoot,
    val proofs: List<MerkleInclusionProof>
)

@JsExport
object CredentialBatches {

    @JvmStatic
    fun batch(
        signedCredentials: List<PrismCredential>
    ): CredentialBatch {
        val merkleProofs = generateProofs(
            signedCredentials.map { it.hash() }
        )

        return CredentialBatch(merkleProofs.root, merkleProofs.proofs)
    }

    @JvmStatic
    fun verifyInclusion(
        signedCredential: PrismCredential,
        merkleRoot: MerkleRoot,
        inclusionProof: MerkleInclusionProof
    ): Boolean {
        return signedCredential.hash() == inclusionProof.hash &&
            verifyProof(merkleRoot, inclusionProof)
    }

    @JvmStatic
    fun computeCredentialBatchId(did: DID, merkleRoot: MerkleRoot): CredentialBatchId {
        val data = CredentialBatchData(did.suffix.value, ByteArr(merkleRoot.hash.value))
        return CredentialBatchId.fromDigest(SHA256Digest.compute(data.encodeToByteArray()))
    }
}
