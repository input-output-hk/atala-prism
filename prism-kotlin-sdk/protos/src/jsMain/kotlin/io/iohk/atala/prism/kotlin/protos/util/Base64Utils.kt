package io.iohk.atala.prism.kotlin.protos.util

actual object Base64Utils {
    actual fun encode(bytes: ByteArray): String {
        val buffer = js("Buffer").from(bytes)
        val result = buffer.toString("base64") as String
        return result.replace('/', '_').replace('+', '-').dropLastWhile { it == '=' }
    }

    actual fun decode(src: String): ByteArray {
        val expectedLength = (src.length + 3) / 4 * 4
        val base64encoded =
            src.replace('_', '/').replace('-', '+').padEnd(expectedLength, '=')
        val decoded = js("Buffer").from(base64encoded, "base64")
        val result = ByteArray(decoded.length as Int)
        for (i in result.indices) {
            result[i] = (decoded[i] as Int).toByte()
        }
        return result
    }
}
