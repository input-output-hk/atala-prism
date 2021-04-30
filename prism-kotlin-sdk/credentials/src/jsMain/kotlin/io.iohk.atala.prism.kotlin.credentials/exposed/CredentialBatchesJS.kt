package io.iohk.atala.prism.kotlin.credentials.exposed

import io.iohk.atala.prism.kotlin.credentials.CredentialBatches
import io.iohk.atala.prism.kotlin.crypto.exposed.MerkleInclusionProofJS
import io.iohk.atala.prism.kotlin.crypto.exposed.MerkleRootJS
import io.iohk.atala.prism.kotlin.crypto.exposed.toKotlin
import io.iohk.atala.prism.kotlin.identity.exposed.DIDJS
import io.iohk.atala.prism.kotlin.identity.exposed.toKotlin

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
            merkleRoot.toKotlin(),
            inclusionProof.internal
        )
    }

    @JsName("computeCredentialBatchId")
    fun computeCredentialBatchId(did: DIDJS, merkleRoot: MerkleRootJS): CredentialBatchIdJS {
        return CredentialBatches.computeCredentialBatchId(did.toKotlin(), merkleRoot.toKotlin()).toJs()
    }
}
