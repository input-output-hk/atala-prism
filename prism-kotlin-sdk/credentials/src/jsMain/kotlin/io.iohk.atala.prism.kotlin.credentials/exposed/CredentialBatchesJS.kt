package io.iohk.atala.prism.kotlin.credentials.exposed

import io.iohk.atala.prism.kotlin.credentials.CredentialBatches
import io.iohk.atala.prism.kotlin.crypto.exposed.MerkleInclusionProofJS
import io.iohk.atala.prism.kotlin.crypto.exposed.MerkleRootJS
import io.iohk.atala.prism.kotlin.crypto.exposed.toJs
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
    fun batch(signedCredentials: Array<CredentialJS>): BatchResult {
        val (root, proofs) = CredentialBatches.batch(signedCredentials.map { it.credential })
        return BatchResult(MerkleRootJS(root.hash.hexValue()), proofs.map { it.toJs() }.toTypedArray())
    }

    fun verifyInclusion(
        signedCredential: CredentialJS,
        merkleRoot: MerkleRootJS,
        inclusionProof: MerkleInclusionProofJS
    ): Boolean {
        return CredentialBatches.verifyInclusion(
            signedCredential.credential,
            merkleRoot.toKotlin(),
            inclusionProof.toKotlin()
        )
    }

    fun computeCredentialBatchId(did: DIDJS, merkleRoot: MerkleRootJS): CredentialBatchIdJS {
        return CredentialBatches.computeCredentialBatchId(did.toKotlin(), merkleRoot.toKotlin()).toJs()
    }
}
