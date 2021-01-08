package io.iohk.atala.prism.kotlin.crypto

typealias Hash = SHA256Digest
// Bitmask index representing leaf position in a tree where unset i-th bit means that the leaf is
// located in the left branch of the i-th node starting from the root and vice-versa
typealias Index = Int

object MerkleTree {

    // In order to defend against second-preimage attack we prefix node hashes with either 0 or 1
    // depending on the type of the node (leaf or internal node)
    val LeafPrefix: Byte = 0
    val NodePrefix: Byte = 1

    // Merkle tree is a tree where every leaf node is labeled with SHA256 hash of some external data
    // block,and every non-leaf node is labeled with SHA256 hash of the concatenation of its child
    // nodes. The tree does not have to be full in the bottom level (i.e. leaves can differ in
    // height), but the maximum leaf height is still limited by O(log(N)) where N is the number of
    // leaves.
    //
    // Note that our MerkleTrees are immutable and hence can only be created by supplying the entire
    // list of leaf nodes' hashes at once.
    private sealed class MerkleTree() {
        abstract val hash: Hash

        data class MerkleNode(val left: MerkleTree, val right: MerkleTree) : MerkleTree() {
            override val hash: Hash = combineHashes(left.hash, right.hash)
        }

        data class MerkleLeaf(val data: Hash) : MerkleTree() {
            override val hash: Hash = prefixHash(data)
        }
    }

    data class MerkleRoot(val hash: Hash)

    // Cryptographic proof of the given hash's inclusion in the Merkle tree which can be verified
    // by anyone.
    data class MerkleInclusionProof(
        val hash: Hash, // hash inclusion of which this proof is for
        val index: Index, // index for the given hash's position in the tree
        val siblings: List<Hash> // given hash's siblings at each level of the tree starting from the bottom
    ) {
        // merkle root of which this proof is for
        @ExperimentalUnsignedTypes
        fun derivedRoot(): MerkleRoot {
            val n = siblings.size
            val root = siblings.indices.fold(
                prefixHash(hash),
                { currentHash, i ->
                    if (index and (1 shl (n - i - 1)) == 0)
                        combineHashes(currentHash, siblings[i])
                    else
                        combineHashes(siblings[i], currentHash)
                }
            )

            return MerkleRoot(root)
        }
    }

    @ExperimentalUnsignedTypes
    private fun combineHashes(left: Hash, right: Hash): Hash =
        SHA256Digest.compute(listOf(NodePrefix) + (left.value + right.value).map { it.toByte() })

    @ExperimentalUnsignedTypes
    private fun prefixHash(data: Hash): Hash =
        SHA256Digest.compute(listOf(LeafPrefix) + data.value.map { it.toByte() })

    fun generateProofs(hashes: List<Hash>): Pair<MerkleRoot, List<MerkleInclusionProof>> {

        fun buildMerkleTree(currentLevel: List<MerkleTree>, nextLevel: List<MerkleTree>): MerkleTree {
            return when {
                currentLevel.size >= 2 -> buildMerkleTree(
                    currentLevel = currentLevel.subList(2, currentLevel.size),
                    nextLevel = listOf(MerkleTree.MerkleNode(currentLevel[0], currentLevel[1])) + nextLevel
                )

                currentLevel.size == 1 -> buildMerkleTree(
                    currentLevel = emptyList(),
                    nextLevel = listOf(currentLevel[0]) + nextLevel
                )

                nextLevel.size == 1 -> nextLevel[0]

                // We reverse `nextLevel` list so that it has the same order as the initial
                // `currentLevel` list
                else -> buildMerkleTree(currentLevel = nextLevel.reversed(), nextLevel = emptyList())
            }
        }

        fun buildProofs(
            tree: MerkleTree,
            currentIndex: Index,
            currentPath: List<Hash>
        ): List<MerkleInclusionProof> {
            return when (tree) {
                is MerkleTree.MerkleLeaf -> listOf(MerkleInclusionProof(tree.data, currentIndex, currentPath))
                is MerkleTree.MerkleNode ->
                    buildProofs(tree.left, currentIndex, listOf(tree.right.hash) + currentPath) +
                        buildProofs(
                            tree.right,
                            currentIndex or (1 shl currentPath.size),
                            listOf(tree.left.hash) + currentPath
                        )
            }
        }

        require(hashes.isNotEmpty())

        val merkleTree = buildMerkleTree(hashes.map { MerkleTree.MerkleLeaf(it) }, emptyList())
        val merkleProofs = buildProofs(merkleTree, 0, emptyList())

        return Pair(MerkleRoot(merkleTree.hash), merkleProofs)
    }

    fun verifyProof(root: MerkleRoot, proof: MerkleInclusionProof): Boolean {
        // Proof length should not exceed 31 as 2^31 is the maximum size of Merkle tree
        return proof.siblings.size < 31 && proof.derivedRoot() == root
    }
}
