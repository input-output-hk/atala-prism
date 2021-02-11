package io.iohk.atala.prism.kotlin.crypto

import com.ionspin.kotlin.bignum.integer.BigInteger
import com.ionspin.kotlin.bignum.integer.Sign
import io.iohk.atala.prism.kotlin.crypto.util.BytesOps.hexToBytes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ECTest {
    val testData = listOf<Byte>(-107, 101, 68, 118, 27, 74, 29, 50, -32, 72, 47, -127, -49, 3, -8, -55, -63, -66, 46, 125)

    @Test
    fun testGeneration() {
        val keyPair = EC.generateKeyPair()
        assertEquals(keyPair.privateKey.getEncoded().size, ECConfig.PRIVATE_KEY_BYTE_SIZE)
        assertEquals(keyPair.privateKey.getHexEncoded().length, ECConfig.PRIVATE_KEY_BYTE_SIZE * 2)
        assertEquals(keyPair.publicKey.getEncoded().size, ECConfig.PUBLIC_KEY_BYTE_SIZE)
        assertEquals(keyPair.publicKey.getHexEncoded().length, ECConfig.PUBLIC_KEY_BYTE_SIZE * 2)
    }

    @Test
    fun testPrivateKeyFromEncoded() {
        val keyPair = EC.generateKeyPair()
        val encodedPrivateKey = keyPair.privateKey.getEncoded()
        val d = BigInteger.fromByteArray(encodedPrivateKey.toByteArray(), Sign.POSITIVE)

        assertEquals(keyPair.privateKey, EC.toPrivateKey(encodedPrivateKey))
        assertEquals(keyPair.privateKey, EC.toPrivateKey(d))
    }

    @Test
    fun testPublicKeyFromEncoded() {
        val keyPair = EC.generateKeyPair()
        val encodedPublicKey = keyPair.publicKey.getEncoded()
        val curvePoint = keyPair.publicKey.getCurvePoint()

        // Modulus for Secp256k1. See https://en.bitcoin.it/wiki/Secp256k1
        val modulus = BigInteger.fromUByteArray(
            hexToBytes("fffffffffffffffffffffffffffffffffffffffffffffffffffffffefffffc2f").toUByteArray(),
            Sign.POSITIVE
        )
        val x = curvePoint.x
        val y = curvePoint.y
        assertTrue((y * y).mod(modulus) == (x * x * x + 7).mod(modulus), "Public key point should follow the elliptic curve equation")

        assertEquals(keyPair.publicKey, EC.toPublicKey(encodedPublicKey))
        assertEquals(keyPair.publicKey, EC.toPublicKey(x, y))
        assertEquals(keyPair.publicKey, EC.toPublicKey(x.toByteArray().toList(), y.toByteArray().toList()))
    }

    @Test
    fun testGeneratePublicKeyFromPrivateKey() {
        val keyPair = EC.generateKeyPair()

        assertEquals(keyPair.publicKey, EC.toPublicKeyFromPrivateKey(keyPair.privateKey))
    }

    @Test
    fun testGenerateSamePrivateKeyAcrossAllImplementations() {
        val hexEncodedPrivateKey = "933c25b9e0b10b0618517edeb389b1b5ba5e781f377af6f573a1af354d008034"

        val privateKey = EC.toPrivateKey(hexToBytes(hexEncodedPrivateKey).map { it.toByte() })

        assertEquals(hexEncodedPrivateKey, privateKey.getHexEncoded())
    }

    @Test
    fun testGenerateSamePublicKeyAcrossAllImplementations() {
        val hexEncodedPublicKey =
            "0477d650217424671208f06ed816dab6c09e6b08c4da0f2f46ead049dd5fbd1c82cd23343346003d4c7faf24ed6314bf340e7882941fd69929526cc889a0f93a1c"

        val publicKey = EC.toPublicKey(hexToBytes(hexEncodedPublicKey).map { it.toByte() })

        assertEquals(hexEncodedPublicKey, publicKey.getHexEncoded())
    }

    @Test
    fun testSignAndVerifyText() {
        val keyPair = EC.generateKeyPair()
        val text = "The quick brown fox jumps over the lazy dog"

        val signature = EC.sign(text, keyPair.privateKey)

        assertTrue(EC.verify(text, keyPair.publicKey, signature))
    }

    @Test
    fun testSignAndVerifyData() {
        val keyPair = EC.generateKeyPair()

        val signature = EC.sign(testData, keyPair.privateKey)

        assertTrue(EC.verify(testData, keyPair.publicKey, signature))
    }

    @Test
    fun testNotVerifyWrongInput() {
        val keyPair = EC.generateKeyPair()
        val wrongKeyPair = EC.generateKeyPair()
        val text = "The quick brown fox jumps over the lazy dog"
        val wrongText = "Wrong text"

        val signature = EC.sign(text, keyPair.privateKey)
        val wrongSignature = EC.sign(wrongText, keyPair.privateKey)

        assertFalse(EC.verify(wrongText, keyPair.publicKey, signature))
        assertFalse(EC.verify(text, wrongKeyPair.publicKey, signature))
        assertFalse(EC.verify(text, keyPair.publicKey, wrongSignature))
    }

    @Test
    fun testVerifySameSignatureInAllImplementations() {
        val hexEncodedPrivateKey = "0123fbf1050c3fc060b709fdcf240e766a41190c40afc5ac7a702961df8313c0"
        val hexEncodedSignature =
            "30450221008a78c557dfc18275b5c800281ef8d26d2b40572b9c1442d708c610f50f797bd302207e44e340f787df7ab1299dabfc988e4c02fcaca0f68dbe813050f4b8641fa739"
        val privateKey = EC.toPrivateKey(hexToBytes(hexEncodedPrivateKey).map { it.toByte() })
        val signature = EC.toSignature(hexToBytes(hexEncodedSignature).map { it.toByte() })

        assertTrue(EC.verify(testData, EC.toPublicKeyFromPrivateKey(privateKey), signature))
    }
}
