package io.iohk.atala.prism.crypto

import cats.data.NonEmptyList

import scala.annotation.tailrec

object MerkleTree {
  type Hash = SHA256Digest
  // Bitmask index representing leaf position in a tree where unset i-th bit means that the leaf is
  // located in the left branch of the i-th node starting from the root and vice-versa
  type Index = Int

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
  private final case class MerkleNode(hash: Hash, left: MerkleTree, right: MerkleTree) extends MerkleTree
  private final case class MerkleLeaf(hash: Hash) extends MerkleTree

  case class MerkleRoot(hash: Hash) extends AnyVal

  // Cryptographic proof of the given hash's inclusion in the Merkle tree which can be verified
  // by anyone.
  case class MerkleInclusionProof(
      hash: Hash, // hash inclusion of which this proof is for
      index: Index, // index for the given hash's position in the tree
      siblings: List[Hash] // given hash's siblings at each level of the tree starting from the bottom
  )

  private def combineHashes(left: Hash, right: Hash): Hash =
    SHA256Digest.compute((left.value ++ right.value).toArray)

  private def combineTrees(left: MerkleTree, right: MerkleTree): MerkleTree =
    MerkleNode(combineHashes(left.hash, right.hash), left, right)

  def generateProofs(hashes: NonEmptyList[Hash]): (MerkleRoot, List[MerkleInclusionProof]) = {
    @tailrec
    def buildMerkleTree(
        currentLevel: List[MerkleTree],
        nextLevel: List[MerkleTree]
    ): MerkleTree = {
      currentLevel match {
        case x :: y :: currentLevelLeftovers =>
          buildMerkleTree(currentLevelLeftovers, combineTrees(x, y) :: nextLevel)
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
        case MerkleLeaf(hash) =>
          List(MerkleInclusionProof(hash, currentIndex, currentPath))
        case MerkleNode(_, left, right) =>
          buildProofs(left, currentIndex, right.hash :: currentPath) ++
            buildProofs(right, currentIndex | (1 << currentPath.size), left.hash :: currentPath)
      }
    }

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
      val calculatedHash = (0 until n).foldLeft(proof.hash) {
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
