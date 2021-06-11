package io.iohk.atala.prism.kotlin.crypto

@JsExport
object MerkleInclusionProofCompanion {
    fun decode(encoded: String): MerkleInclusionProof =
        MerkleInclusionProof.decode(encoded)
}
