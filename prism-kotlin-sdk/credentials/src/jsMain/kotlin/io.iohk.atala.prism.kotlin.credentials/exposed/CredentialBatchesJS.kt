package io.iohk.atala.prism.kotlin.credentials.exposed

import io.iohk.atala.prism.kotlin.credentials.CredentialBatches
import io.iohk.atala.prism.kotlin.crypto.*
import io.iohk.atala.prism.kotlin.crypto.exposed.MerkleInclusionProofJS
import io.iohk.atala.prism.kotlin.crypto.exposed.MerkleRootJS
import io.iohk.atala.prism.kotlin.crypto.util.BytesOps.hexToBytes

@JsExport
data class BatchResult(
    val root: MerkleRootJS,
    val proofs: Array<MerkleInclusionProofJS>
)

@JsExport
object CredentialBatchesJS {
    @JsName("batch")
    fun batch(signedCredentials: Array<CredentialJS>): BatchResult {
        val (root, proofs) = CredentialBatches.batch(signedCredentials.map { it.credential })
        return BatchResult(MerkleRootJS(root.hash.hexValue()), proofs.map { MerkleInclusionProofJS(it) }.toTypedArray())
    }

    @JsName("verifyInclusion")
    fun verifyInclusion(
        signedCredential: CredentialJS,
        merkleRoot: MerkleRootJS,
        inclusionProof: MerkleInclusionProofJS
    ): Boolean {
        return CredentialBatches.verifyInclusion(
            signedCredential.credential,
            MerkleRoot(Hash(hexToBytes(merkleRoot.hash))),
            inclusionProof.internal
        )
    }
}
