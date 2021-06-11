package io.iohk.atala.prism.kotlin.crypto.exposed

import io.iohk.atala.prism.kotlin.crypto.SHA256Digest

fun SHA256Digest.toJs(): SHA256DigestJS =
    SHA256DigestJS(value)

fun SHA256DigestJS.toKotlin(): SHA256Digest =
    SHA256Digest(value)

@JsExport
object SHA256DigestJSCompanion {
    val BYTE_LENGTH = SHA256Digest.BYTE_LENGTH
    val HEX_STRING_RE = SHA256Digest.HEX_STRING_RE.toString()
    val HEX_STRING_LENGTH = SHA256Digest.HEX_STRING_LENGTH

    fun compute(bytes: ByteArray): SHA256DigestJS =
        SHA256Digest.compute(bytes).toJs()

    fun fromHex(string: String): SHA256DigestJS =
        SHA256Digest.fromHex(string).toJs()

    fun fromBytes(bytes: ByteArray): SHA256DigestJS =
        SHA256Digest.fromHex(bytes).toJs()
}

@JsExport
data class SHA256DigestJS(val value: ByteArray) {
    fun hexValue(): String = toKotlin().hexValue()
}
