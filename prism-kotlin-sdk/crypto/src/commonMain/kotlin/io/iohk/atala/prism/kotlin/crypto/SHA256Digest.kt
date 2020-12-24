package io.iohk.atala.prism.kotlin.crypto

import io.iohk.atala.prism.kotlin.util.BytesOps

@ExperimentalUnsignedTypes
data class SHA256Digest(val value: List<UByte>) {
    companion object {
        val BYTE_LENGTH = 32
        val HEX_STRING_RE = Regex("^(?:[0-9a-fA-F]{2})+$")
        val HEX_STRING_LENGTH = 64

        fun compute(bytes: List<Byte>): SHA256Digest {
            return SHA256Digest(SHA256.compute(bytes.toByteArray().toUByteArray()).toList())
        }

        fun fromHex(string: String): SHA256Digest {
            require(HEX_STRING_RE.matches(string))
            return SHA256Digest(BytesOps.hexToBytes(string))
        }

        fun fromHex(bytes: List<Byte>): SHA256Digest {
            require(bytes.size == HEX_STRING_LENGTH)
            return fromHex(bytes.toByteArray().decodeToString())
        }
    }

    fun hexValue(): String = BytesOps.bytesToHex(value)
}
