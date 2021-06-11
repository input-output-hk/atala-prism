package io.iohk.atala.prism.kotlin.crypto

import com.ionspin.kotlin.bignum.integer.BigInteger
import io.iohk.atala.prism.kotlin.crypto.keys.ECKeyPair
import io.iohk.atala.prism.kotlin.crypto.keys.ECPoint
import io.iohk.atala.prism.kotlin.crypto.keys.ECPrivateKey
import io.iohk.atala.prism.kotlin.crypto.keys.ECPublicKey
import io.iohk.atala.prism.kotlin.crypto.signature.ECSignature

expect object EC {
    /**
     * Generates a secp256k1 key-pair.
     */
    fun generateKeyPair(): ECKeyPair

    /**
     * Returns the private key represented by the given byte array.
     */
    fun toPrivateKey(encoded: ByteArray): ECPrivateKey

    /**
     * Returns the private key represented by the given number.
     */
    fun toPrivateKey(d: BigInteger): ECPrivateKey

    /**
     * Returns the public key represented by the given encoded byte array.
     */
    fun toPublicKey(encoded: ByteArray): ECPublicKey

    /**
     * Returns the public key represented by the given coordinates as byte arrays.
     */
    fun toPublicKey(x: ByteArray, y: ByteArray): ECPublicKey

    /**
     * Returns the public key represented by the given coordinates.
     */
    fun toPublicKey(x: BigInteger, y: BigInteger): ECPublicKey

    /**
     * Returns the public key represented by the given private key.
     */
    fun toPublicKeyFromPrivateKey(privateKey: ECPrivateKey): ECPublicKey

    /**
     * Returns the signature represented by the given encoded byte array.
     */
    fun toSignature(encoded: ByteArray): ECSignature

    /**
     * Signs the given text with the given private key.
     */
    fun sign(text: String, privateKey: ECPrivateKey): ECSignature

    /**
     * Signs the given data with the given private key.
     */
    fun sign(data: ByteArray, privateKey: ECPrivateKey): ECSignature

    /**
     * Verifies whether the given text matches the given signature with the given public key.
     */
    fun verify(text: String, publicKey: ECPublicKey, signature: ECSignature): Boolean

    /**
     * Verifies whether the given data matches the given signature with the given public key.
     */
    fun verify(data: ByteArray, publicKey: ECPublicKey, signature: ECSignature): Boolean

    /**
     * Verifies whether the given point belongs to the secp256k1 curve.
     */
    fun isSecp256k1(point: ECPoint): Boolean
}
