package io.iohk.atala.prism.crypto

import cats.data.NonEmptyList
import org.scalacheck.{Gen, Shrink}
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks._

import scala.math.{ceil, log}

class MerkleTreeSpec extends AnyWordSpec {
  implicit def noShrink[T]: Shrink[T] = Shrink.shrinkAny

  val hashGen: Gen[SHA256Digest] =
    Gen.asciiStr.map(str => SHA256Digest.compute(str.getBytes))
  val hashNonEmptyListGen: Gen[NonEmptyList[SHA256Digest]] =
    Gen.nonEmptyListOf(hashGen).map(NonEmptyList.fromListUnsafe)

  "MerkleTree" should {
    "build proofs for all supplied hashes" in {
      forAll(hashNonEmptyListGen) { hashes: NonEmptyList[SHA256Digest] =>
        val (_, proofs) = MerkleTree.generateProofs(hashes)

        assert(hashes.forall(h => proofs.exists(_.hash == h)))
      }
    }

    "build proofs of limited length" in {
      forAll(hashNonEmptyListGen) { hashes: NonEmptyList[SHA256Digest] =>
        val (_, proofs) = MerkleTree.generateProofs(hashes)

        val maxLength = ceil(log(hashes.length) / log(2.0)).toInt
        assert(proofs.forall(_.siblings.length <= maxLength))
      }
    }

    "build verifiable proofs" in {
      forAll(hashNonEmptyListGen) { hashes: NonEmptyList[SHA256Digest] =>
        val (root, proofs) = MerkleTree.generateProofs(hashes)

        for (proof <- proofs) {
          MerkleTree.verifyProof(root, proof) mustBe true
        }
      }
    }

    "reject invalid proofs" in {
      forAll(hashNonEmptyListGen.suchThat(_.size > 1)) { hashes =>
        val (root, proofs) = MerkleTree.generateProofs(hashes)
        val proofNumber = Gen.chooseNum[Int](0, proofs.size - 1).sample.get
        val proof = proofs(proofNumber)
        val relevantMask = (1 << proof.siblings.size) - 1 // The first N bits of index that matter
        forAll(
          hashGen.suchThat(_ != proof.hash),
          Gen.chooseNum[Int](0, relevantMask),
          hashNonEmptyListGen.suchThat(_.toList != proof.siblings)
        ) { (otherHash, otherIndex, otherHashes) =>
          whenever(otherIndex != proof.index) {
            val invalidProof1 = proof.copy(hash = otherHash)
            val invalidProof2 = proof.copy(index = otherIndex)
            val invalidProof3 = proof.copy(siblings = otherHashes.toList)

            MerkleTree.verifyProof(root, invalidProof1) mustBe false
            MerkleTree.verifyProof(root, invalidProof2) mustBe false
            MerkleTree.verifyProof(root, invalidProof3) mustBe false
          }
        }
      }
    }
  }
}
