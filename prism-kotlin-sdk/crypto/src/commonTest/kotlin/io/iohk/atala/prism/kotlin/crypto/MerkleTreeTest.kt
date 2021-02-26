package io.iohk.atala.prism.kotlin.crypto

import kotlin.math.ceil
import kotlin.math.log2
import kotlin.random.Random
import kotlin.test.*

class MerkleTreeTest {

    private fun randomHexString(length: Int): String = List(length) {
        (('a'..'f') + ('0'..'9')).random()
    }.joinToString("")

    private fun randomHash(length: Int) = SHA256Digest.fromHex(randomHexString(length))

    private val hashes = (1..50).map { randomHash(length = 16) }

    @Test
    fun buildProofsForAllSuppliedHashes() {
        val (_, proofs) = generateProofs(hashes)

        hashes.forEach { h -> assertNotNull(proofs.find { it.hash == h }) }
    }

    @Test
    fun buildProofsOfLimitedLength() {
        val (_, proofs) = generateProofs(hashes)

        val maxLength = ceil(log2(hashes.size.toDouble()) / log2(2.0)).toInt()
        proofs.forEach { assertTrue(it.siblings.size <= maxLength) }
    }

    @Test
    fun buildVerifiableProofs() {
        val (root, proofs) = generateProofs(hashes)

        for (proof in proofs) {
            assertTrue(verifyProof(root, proof))
        }
    }

    @Test
    fun rejectInvalidProofs() {
        val (root, proofs) = generateProofs(hashes)
        val proofNumber = Random.nextInt(proofs.size)
        val proof = proofs[proofNumber]
        val relevantMask = (1 shl proof.siblings.size) - 1 // The first N bits of index that matter

        val invalidProof1 = proof.copy(hash = randomHash(8))

        val randomIndex = Random.nextInt(relevantMask)
        val differentIndex = if (randomIndex == proof.index) randomIndex + 1 else randomIndex
        val invalidProof2 = proof.copy(index = differentIndex)

        val invalidProof3 = proof.copy(siblings = (1..30).map { randomHash(8) })

        assertFalse(verifyProof(root, invalidProof1))
        assertFalse(verifyProof(root, invalidProof2))
        assertFalse(verifyProof(root, invalidProof3))
    }

    @Test
    fun beResistantToSecondPreimageAttacks() {
        val (root, proofs) = generateProofs(hashes)
        val proofNumber = Random.nextInt(proofs.size)
        val proof = proofs[proofNumber]

        val firstSibling = proof.siblings.first()
        val newHash =
            SHA256Digest.compute(
                listOf(NodePrefix) + (firstSibling.value + proof.hash.value).map { it.toByte() }
            )
        val newSiblings = proof.siblings.drop(1)
        val newIndex = proof.index shl 1
        val newProof = proof.copy(hash = newHash, index = newIndex, siblings = newSiblings)

        assertFalse(verifyProof(root, newProof))
    }

    @Test
    fun deriveConsistentRoot() {
        val (root, proofs) = generateProofs(hashes)

        proofs.forEach { assertEquals(it.derivedRoot(), root) }
    }
}
