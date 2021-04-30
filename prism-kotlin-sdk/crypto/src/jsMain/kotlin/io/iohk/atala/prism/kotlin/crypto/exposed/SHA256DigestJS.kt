package io.iohk.atala.prism.kotlin.crypto.exposed

import io.iohk.atala.prism.kotlin.crypto.SHA256Digest
import io.iohk.atala.prism.kotlin.crypto.util.toByteArray

fun SHA256Digest.toJs(): SHA256DigestJS =
    SHA256DigestJS(value.toByteArray())

fun SHA256DigestJS.toKotlin(): SHA256Digest =
    SHA256Digest(value.toList().map { it.toUByte() })

@JsExport
object SHA256DigestJSCompanion {
    val BYTE_LENGTH = SHA256Digest.BYTE_LENGTH
    val HEX_STRING_RE = SHA256Digest.HEX_STRING_RE.toString()
    val HEX_STRING_LENGTH = SHA256Digest.HEX_STRING_LENGTH

    @JsName("compute")
    fun compute(bytes: ByteArray): SHA256DigestJS =
        SHA256Digest.compute(bytes.toList()).toJs()

    @JsName("fromHex")
    fun fromHex(string: String): SHA256DigestJS =
        SHA256Digest.fromHex(string).toJs()

    @JsName("fromBytes")
    fun fromBytes(bytes: ByteArray): SHA256DigestJS =
        SHA256Digest.fromHex(bytes.toList()).toJs()
}

@JsExport
data class SHA256DigestJS(val value: ByteArray) {
    @JsName("hexValue")
    fun hexValue(): String = toKotlin().hexValue()
}
