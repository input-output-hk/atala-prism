package io.iohk.atala.prism.kotlin.crypto.derivation

import io.iohk.atala.prism.kotlin.crypto.util.BytesOps
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.test.*

class JvmKeyDerivationTest {

    @Test
    fun testRandomMnemonicCode() {
        for (i in 1..10) {
            assertEquals(12, JvmKeyDerivation.randomMnemonicCode().words.size)
        }
    }

    @Test
    fun testGenerateRandomMnemonics() {
        val seenWords = mutableSetOf<String>()
        for (i in 1..300) {
            seenWords.addAll(JvmKeyDerivation.randomMnemonicCode().words)
        }

        // with great probability we'll see at least 75% of words after 3600 draws from 2048 possible   
        assertTrue(2048 - seenWords.size < 512)
    }

    @Test
    fun testValidMnemonicCode() {
        for (word in JvmKeyDerivation.getValidMnemonicWords()) {
            assertTrue(JvmKeyDerivation.isValidMnemonicWord(word))
        }
    }

    @Test
    fun testInvalidMnemonicCode() {
        assertFalse(JvmKeyDerivation.isValidMnemonicWord("hocus"))
    }

    @Test
    fun testComputeRightBinarySeed() {
        val password = "TREZOR"
        val jsonData = javaClass.classLoader!!.getResource("bip39_vectors.json")!!.readText()
        val vectors = Json.decodeFromString<List<List<String>>>(jsonData)
        for (v in vectors) {
            val (_, mnemonicPhrase, binarySeedHex, _) = v
            val mnemonicCode = MnemonicCode(mnemonicPhrase.split(" "))
            val binarySeed = JvmKeyDerivation.binarySeed(mnemonicCode, password)

            assertEquals(binarySeedHex, BytesOps.bytesToHex(binarySeed.map { it.toUByte() }))
        }
    }

    @Test
    fun testFailWhenChecksumIsIncorrect() {
        val mnemonicCode = MnemonicCode(List(15) { "abandon" })
        assertFailsWith<MnemonicChecksumException> {
            JvmKeyDerivation.binarySeed(mnemonicCode, "")
        }
    }

    @Test
    fun testFailWhenInvalidWordIsUsed() {
        val mnemonicCode = MnemonicCode(listOf("hocus", "pocus", "mnemo", "codus") + List(11) { "abandon" })
        assertFailsWith<MnemonicWordException> {
            JvmKeyDerivation.binarySeed(mnemonicCode, "")
        }
    }

    @Test
    fun testFailWhenWrongLength() {
        val mnemonicCode = MnemonicCode(listOf("abandon"))
        assertFailsWith<MnemonicLengthException> {
            JvmKeyDerivation.binarySeed(mnemonicCode, "")
        }
    }

    @Test
    fun testDeriveKey() {
        @Serializable
        class RawTestVector(val seed: String, val derivations: List<List<String>>)
        class Derivation(val path: String, val pubKeyHex: String, val privKeyHex: String)
        class TestVector(val seedHex: String, val derivations: List<Derivation>)

        val jsonData = javaClass.classLoader!!.getResource("bip32_vectors.json")!!.readText()
        val vectors = Json.decodeFromString<List<RawTestVector>>(jsonData).map { vector ->
            val derivations = vector.derivations.map { derivation ->
                val (path, pubKeyHex, privKeyHex) = derivation
                Derivation(path, pubKeyHex, privKeyHex)
            }
            TestVector(vector.seed, derivations)
        }

        for (v in vectors) {
            val seed = BytesOps.hexToBytes(v.seedHex).map { it.toByte() }
            for (d in v.derivations) {
                val path = DerivationPath.fromPath(d.path)
                val key = JvmKeyDerivation.deriveKey(seed, path)

                assertEquals(d.privKeyHex, BytesOps.bytesToHex(key.privateKey().getEncoded().map { it.toUByte() }))
                assertEquals(d.pubKeyHex, BytesOps.bytesToHex(key.publicKey().getEncoded().map { it.toUByte() }))
            }
        }
    }
}
