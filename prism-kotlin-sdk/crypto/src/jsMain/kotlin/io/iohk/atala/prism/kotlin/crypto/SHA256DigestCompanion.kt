package io.iohk.atala.prism.kotlin.crypto

@JsExport
object SHA256DigestCompanion {
    val BYTE_LENGTH = SHA256Digest.BYTE_LENGTH
    val HEX_STRING_RE = SHA256Digest.HEX_STRING_RE.toString()
    val HEX_STRING_LENGTH = SHA256Digest.HEX_STRING_LENGTH

    fun compute(bytes: ByteArray): SHA256Digest =
        SHA256Digest.compute(bytes)

    fun fromHex(string: String): SHA256Digest =
        SHA256Digest.fromHex(string)

    fun fromBytes(bytes: ByteArray): SHA256Digest =
        SHA256Digest.fromBytes(bytes)
}
