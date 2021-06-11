package io.iohk.atala.prism.kotlin.protos.util

expect object Base64Utils {
    /**
     * Returns a URL-safe Base64 encoding without padding (i.e. no trailing '='s).
     */
    fun encode(bytes: ByteArray): String

    /**
     * Decodes a URL-safe Base64 encoding without padding into a list of bytes.
     */
    fun decode(src: String): ByteArray
}
