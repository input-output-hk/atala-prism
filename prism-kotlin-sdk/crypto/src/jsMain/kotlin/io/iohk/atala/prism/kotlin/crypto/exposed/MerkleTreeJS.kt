package io.iohk.atala.prism.kotlin.crypto.exposed

import io.iohk.atala.prism.kotlin.crypto.*

fun MerkleRootJS.toKotlin(): MerkleRoot =
    MerkleRoot(SHA256Digest.fromHex(hash))

@JsExport
data class MerkleRootJS(val hash: String)

@JsExport
object MerkleInclusionProofJSCompanion {
    @JsName("decode")
    fun decode(encoded: String): MerkleInclusionProofJS =
        MerkleInclusionProofJS(MerkleInclusionProof.decode(encoded))
}

@JsExport
data class MerkleInclusionProofJS constructor(val internal: MerkleInclusionProof) {
    val hash: String = internal.hash.hexValue()
    val index: Index = internal.index
    val siblings: Array<String> = internal.siblings.map { it.hexValue() }.toTypedArray()

    @JsName("derivedRoot")
    fun derivedRoot(): MerkleRootJS =
        MerkleRootJS(internal.derivedRoot().hash.hexValue())

    @JsName("encode")
    fun encode(): String =
        internal.encode()
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
    return verifyProof(root.toKotlin(), proof.internal)
}
