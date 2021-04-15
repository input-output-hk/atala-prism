package io.iohk.atala.prism.kotlin.crypto.exposed

import io.iohk.atala.prism.kotlin.crypto.*

@JsExport
data class MerkleRootJS(val hash: String)

@JsExport
data class MerkleInclusionProofJS constructor(val internal: MerkleInclusionProof) {
    val hash: String = internal.hash.hexValue()
    val index: Index = internal.index
    val siblings: Array<String> = internal.siblings.map { it.hexValue() }.toTypedArray()

    @JsName("derivedRoot")
    fun derivedRoot(): MerkleRootJS =
        MerkleRootJS(internal.derivedRoot().hash.hexValue())
}

@JsExport
data class MerklePairJS(val root: MerkleRootJS, val proofs: Array<MerkleInclusionProofJS>)

@JsExport
@JsName("generateProofsJS")
fun generateProofsJS(hashes: Array<String>): MerklePairJS {
    val (root, proofs) = generateProofs(hashes.map { SHA256Digest.fromHex(it) })

    return MerklePairJS(MerkleRootJS(root.hash.hexValue()), proofs.map { MerkleInclusionProofJS(it) }.toTypedArray())
}

@JsExport
@JsName("verifyProofJS")
fun verifyProofJS(root: MerkleRootJS, proof: MerkleInclusionProofJS): Boolean {
    return verifyProof(MerkleRoot(SHA256Digest.fromHex(root.hash)), proof.internal)
}
