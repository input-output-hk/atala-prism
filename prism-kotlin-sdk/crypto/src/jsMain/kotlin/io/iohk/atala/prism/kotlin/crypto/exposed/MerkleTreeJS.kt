package io.iohk.atala.prism.kotlin.crypto.exposed

import io.iohk.atala.prism.kotlin.crypto.*

fun MerkleRootJS.toKotlin(): MerkleRoot =
    MerkleRoot(SHA256Digest.fromHex(hash))

fun MerkleRoot.toKotlin(): MerkleRootJS =
    MerkleRootJS(this.hash.hexValue())

fun MerkleInclusionProofJS.toKotlin(): MerkleInclusionProof =
    this.proof

fun MerkleInclusionProof.toJs(): MerkleInclusionProofJS =
    MerkleInclusionProofJS(this)

@JsExport
data class MerkleRootJS(val hash: String)

@JsExport
object MerkleInclusionProofJSCompanion {
    fun decode(encoded: String): MerkleInclusionProofJS =
        MerkleInclusionProofJS(MerkleInclusionProof.decode(encoded))
}

@JsExport
data class MerkleInclusionProofJS internal constructor(internal val proof: MerkleInclusionProof) {
    @JsName("create")
    constructor(hash: SHA256DigestJS, index: Index, siblings: Array<SHA256DigestJS>) :
        this(MerkleInclusionProof(hash.toKotlin(), index, siblings.map { it.toKotlin() }))

    val hash: String = proof.hash.hexValue()
    val index: Index = proof.index
    val siblings: Array<String> = proof.siblings.map { it.hexValue() }.toTypedArray()

    fun derivedRoot(): MerkleRootJS =
        MerkleRootJS(proof.derivedRoot().hash.hexValue())

    fun encode(): String =
        proof.encode()
}

@JsExport
data class MerklePairJS(val root: MerkleRootJS, val proofs: Array<MerkleInclusionProofJS>)

@JsExport
fun generateProofsJS(hashes: Array<String>): MerklePairJS {
    val (root, proofs) = generateProofs(hashes.map { SHA256Digest.fromHex(it) })

    return MerklePairJS(MerkleRootJS(root.hash.hexValue()), proofs.map { MerkleInclusionProofJS(it) }.toTypedArray())
}

@JsExport
fun verifyProofJS(root: MerkleRootJS, proof: MerkleInclusionProofJS): Boolean {
    return verifyProof(root.toKotlin(), proof.proof)
}
