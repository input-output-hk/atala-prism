package io.iohk.atala.prism.kotlin.crypto

import kotlinx.serialization.json.*
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.random.Random
import kotlin.test.*

class MerkleTreeTest {

    private fun randomHexString(length: Int): String = List(length) {
        (('a'..'f') + ('0'..'9')).random()
    }.joinToString("")

    private fun randomHash(length: Int) = SHA256Digest.fromHex(randomHexString(length))

    private val hashes = (1..50).map { randomHash(length = SHA256Digest.HEX_STRING_LENGTH) }

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

        val invalidProof1 = proof.copy(hash = randomHash(SHA256Digest.HEX_STRING_LENGTH))

        val randomIndex = Random.nextInt(relevantMask)
        val differentIndex = if (randomIndex == proof.index) randomIndex + 1 else randomIndex
        val invalidProof2 = proof.copy(index = differentIndex)

        val invalidProof3 = proof.copy(siblings = (1..30).map { randomHash(SHA256Digest.HEX_STRING_LENGTH) })

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
                byteArrayOf(NodePrefix) + (firstSibling.value + proof.hash.value)
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

    @Test
    fun encodingHasTheNecessaryFields() {
        val (_, proofs) = generateProofs(hashes)
        val encoded = proofs[0].encode()
        val json = Json.parseToJsonElement(encoded).jsonObject

        assertNotNull(
            json[MerkleInclusionProof.hashField]?.jsonPrimitive?.contentOrNull,
            "Hash must be present and represented by a string"
        )
        assertNotNull(
            json[MerkleInclusionProof.indexField]?.jsonPrimitive?.int,
            "Index must be present and represented by an integer"
        )
        assertNotNull(
            json[MerkleInclusionProof.siblingsField]?.jsonArray,
            "Siblings must be present and represented by an array"
        )
    }

    @Test
    fun decodingCanHandleSpecifiedJson() {
        val hash = "a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3"
        val index = 10
        val siblings = listOf(
            "8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92",
            "15e2b0d3c33891ebb0f1ef609ec419420c20e320ce94c65fbc8c3312448eb225",
            "c775e7b757ede630cd0aa1113bd102661ab38829ca52a6422ab782862f268646"
        )
        val decoded = MerkleInclusionProof.decode(
            """
                {
                    "${MerkleInclusionProof.hashField}": "$hash",
                    "${MerkleInclusionProof.indexField}": $index,
                    "${MerkleInclusionProof.siblingsField}": [
                        ${siblings.joinToString(",")}
                    ]
                }
            """.trimIndent()
        )

        assertEquals(hash, decoded.hash.hexValue())
        assertEquals(index, decoded.index)
        assertEquals(siblings, decoded.siblings.map { it.hexValue() })
    }

    @Test
    fun encodingIsReversibleByDecoding() {
        val (_, proofs) = generateProofs(hashes)

        proofs.forEach {
            assertEquals(it, MerkleInclusionProof.decode(it.encode()))
        }
    }
}
