package io.iohk.atala.prism.kotlin.crypto

import io.iohk.atala.prism.kotlin.crypto.util.BytesOps
import kotlin.js.JsExport
import kotlin.jvm.JvmStatic

@JsExport
data class SHA256Digest(val value: ByteArray) {
    fun hexValue(): String = BytesOps.bytesToHex(value)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as SHA256Digest

        if (!value.contentEquals(other.value)) return false

        return true
    }

    override fun hashCode(): Int {
        return value.contentHashCode()
    }

    companion object {
        val BYTE_LENGTH = 32
        val HEX_STRING_RE = Regex("^(?:[0-9a-fA-F]{2})+$")
        val HEX_STRING_LENGTH = 64

        @JvmStatic
        fun compute(bytes: ByteArray): SHA256Digest {
            return SHA256Digest(SHA256.compute(bytes))
        }

        @JvmStatic
        fun fromHex(string: String): SHA256Digest {
            require(HEX_STRING_RE.matches(string) && string.length == HEX_STRING_LENGTH)
            return SHA256Digest(BytesOps.hexToBytes(string))
        }

        @JvmStatic
        fun fromBytes(bytes: ByteArray): SHA256Digest {
            require(bytes.size == HEX_STRING_LENGTH / 2)
            return SHA256Digest(bytes)
        }
    }
}
