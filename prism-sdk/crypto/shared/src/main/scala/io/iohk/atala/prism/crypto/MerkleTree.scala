package io.iohk.atala.prism.crypto

import scala.annotation.tailrec

object MerkleTree {
  type Hash = SHA256Digest
  // Bitmask index representing leaf position in a tree where unset i-th bit means that the leaf is
  // located in the left branch of the i-th node starting from the root and vice-versa
  type Index = Int

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
  private sealed trait MerkleTree {
    def hash: Hash
  }
  private final case class MerkleNode(left: MerkleTree, right: MerkleTree) extends MerkleTree {
    val hash: Hash = combineHashes(left.hash, right.hash)
  }
  private final case class MerkleLeaf(data: Hash) extends MerkleTree {
    val hash: Hash = prefixHash(data)
  }

  case class MerkleRoot(hash: Hash) extends AnyVal

  // Cryptographic proof of the given hash's inclusion in the Merkle tree which can be verified
  // by anyone.
  case class MerkleInclusionProof(
      hash: Hash, // hash inclusion of which this proof is for
      index: Index, // index for the given hash's position in the tree
      siblings: List[Hash] // given hash's siblings at each level of the tree starting from the bottom
  )

  private def combineHashes(left: Hash, right: Hash): Hash =
    SHA256Digest.compute((NodePrefix +: (left.value ++ right.value)).toArray)

  private def prefixHash(data: Hash): Hash =
    SHA256Digest.compute((LeafPrefix +: data.value).toArray)

  def generateProofs(hashes: List[Hash]): (MerkleRoot, List[MerkleInclusionProof]) = {
    @tailrec
    def buildMerkleTree(
        currentLevel: List[MerkleTree],
        nextLevel: List[MerkleTree]
    ): MerkleTree = {
      currentLevel match {
        case x :: y :: currentLevelLeftovers =>
          buildMerkleTree(currentLevelLeftovers, MerkleNode(x, y) :: nextLevel)
        case x :: Nil =>
          buildMerkleTree(Nil, x :: nextLevel)
        case Nil =>
          nextLevel match {
            case List(root) =>
              root
            case _ =>
              // We reverse `nextLevel` list so that it has the same order as the initial
              // `currentLevel` list
              buildMerkleTree(nextLevel.reverse, Nil)
          }
      }
    }

    def buildProofs(
        tree: MerkleTree,
        currentIndex: Index,
        currentPath: List[Hash]
    ): List[MerkleInclusionProof] = {
      tree match {
        case MerkleLeaf(data) =>
          List(MerkleInclusionProof(data, currentIndex, currentPath))
        case MerkleNode(left, right) =>
          buildProofs(left, currentIndex, right.hash :: currentPath) ++
            buildProofs(right, currentIndex | (1 << currentPath.size), left.hash :: currentPath)
      }
    }

    require(hashes.nonEmpty)

    val merkleTree = buildMerkleTree(hashes.map(MerkleLeaf.apply).toList, Nil)
    val merkleProofs = buildProofs(merkleTree, 0, Nil)
    (MerkleRoot(merkleTree.hash), merkleProofs)
  }

  def verifyProof(root: MerkleRoot, proof: MerkleInclusionProof): Boolean = {
    val n = proof.siblings.size
    // Proof length should not exceed 31 as 2^31 is the maximum size of Merkle tree
    if (n > 31) {
      false
    } else {
      val calculatedHash = (0 until n).foldLeft(prefixHash(proof.hash)) {
        case (currentHash, i) =>
          if ((proof.index & (1 << (n - i - 1))) == 0) {
            combineHashes(currentHash, proof.siblings(i))
          } else {
            combineHashes(proof.siblings(i), currentHash)
          }
      }

      calculatedHash == root.hash
    }
  }
}
