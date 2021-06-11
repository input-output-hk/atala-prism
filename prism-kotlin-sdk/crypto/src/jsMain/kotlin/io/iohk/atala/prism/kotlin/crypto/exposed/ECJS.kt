package io.iohk.atala.prism.kotlin.crypto.exposed

import com.ionspin.kotlin.bignum.integer.BigInteger
import io.iohk.atala.prism.kotlin.crypto.EC

/* Exportable Kotlin.js types are limited at the time to the following:
 * - dynamic, Any, String, Boolean, Byte, Short, Int, Float, Double
 * - BooleanArray, ByteArray, ShortArray, IntArray, FloatArray, DoubleArray
 * - Array<exportable-type>
 * - Function types with exportable parameters and return types
 * - external or @JsExport classes and interfaces
 * - Nullable counterparts of types above
 *
 * Hence we need a separate exportable API for Javascript users, which this is.
 */

@JsExport
object ECJS {
    fun generateKeyPair(): ECKeyPairJS {
        val keyPair = EC.generateKeyPair()
        return ECKeyPairJS(ECPublicKeyJS(keyPair.publicKey), ECPrivateKeyJS(keyPair.privateKey))
    }

    fun toPrivateKeyFromBytes(encoded: ByteArray): ECPrivateKeyJS =
        EC.toPrivateKey(encoded).toJs()

    fun toPrivateKeyFromBigInteger(d: String): ECPrivateKeyJS =
        EC.toPrivateKey(BigInteger.parseString(d)).toJs()

    fun toPublicKeyFromBytes(encoded: ByteArray): ECPublicKeyJS =
        EC.toPublicKey(encoded).toJs()

    fun toPublicKeyFromBigIntegerCoordinates(x: String, y: String): ECPublicKeyJS =
        EC.toPublicKey(BigInteger.parseString(x), BigInteger.parseString(y)).toJs()

    fun toPublicKeyFromPrivateKey(privateKey: ECPrivateKeyJS): ECPublicKeyJS =
        EC.toPublicKeyFromPrivateKey(privateKey.toKotlin()).toJs()

    fun toSignature(encoded: ByteArray): String =
        EC.toSignature(encoded).getHexEncoded()

    fun signBytes(
        bytes: ByteArray,
        privateKey: ECPrivateKeyJS
    ): ECSignatureJS =
        EC.sign(bytes, privateKey.toKotlin()).toJs()

    fun sign(
        text: String,
        privateKey: ECPrivateKeyJS
    ): ECSignatureJS =
        EC.sign(text, privateKey.toKotlin()).toJs()

    fun verifyBytes(
        bytes: ByteArray,
        publicKey: ECPublicKeyJS,
        signature: ECSignatureJS
    ): Boolean =
        EC.verify(bytes, publicKey.toKotlin(), signature.toKotlin())

    fun verify(
        text: String,
        publicKey: ECPublicKeyJS,
        signature: ECSignatureJS
    ): Boolean =
        EC.verify(text, publicKey.toKotlin(), signature.toKotlin())
}
