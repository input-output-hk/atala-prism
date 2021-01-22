package io.iohk.atala.prism.kotlin.crypto.exposed

import com.ionspin.kotlin.bignum.integer.BigInteger
import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.crypto.signature.ECSignature
import io.iohk.atala.prism.kotlin.util.BytesOps.hexToBytes

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

@ExperimentalJsExport
@ExperimentalUnsignedTypes
@JsExport
object ECJS {
    fun generateKeyPair(): ECKeyPairJS {
        val keyPair = EC.generateKeyPair()
        return ECKeyPairJS(keyPair.publicKey.getHexEncoded(), keyPair.privateKey.getHexEncoded())
    }

    fun toPrivateKeyFromBytes(encoded: ByteArray): String =
        EC.toPrivateKey(encoded.toList()).getHexEncoded()

    fun toPrivateKeyFromBigInteger(d: String): String =
        EC.toPrivateKey(BigInteger.parseString(d)).getHexEncoded()

    fun toPublicKeyFromBytes(encoded: ByteArray): String =
        EC.toPublicKey(encoded.toList()).getHexEncoded()

    fun toPublicKeyFromBigIntegerCoordinates(x: String, y: String): String =
        EC.toPublicKey(BigInteger.parseString(x), BigInteger.parseString(y)).getHexEncoded()

    fun toPublicKeyFromPrivateKey(privateKey: String): String {
        val bytes = hexToBytes(privateKey).map { it.toByte() }
        return EC.toPublicKeyFromPrivateKey(EC.toPrivateKey(bytes)).getHexEncoded()
    }

    fun toSignature(encoded: ByteArray): String =
        EC.toSignature(encoded.toList()).getHexEncoded()

    fun sign(
        text: String,
        privateKey: String
    ): String =
        EC.sign(text, EC.toPrivateKey(hexToBytes(privateKey).map { it.toByte() })).getHexEncoded()

    fun verify(
        text: String,
        publicKey: String,
        signature: String
    ): Boolean =
        EC.verify(text, EC.toPublicKey(hexToBytes(publicKey).map { it.toByte() }), ECSignature(signature))
}
