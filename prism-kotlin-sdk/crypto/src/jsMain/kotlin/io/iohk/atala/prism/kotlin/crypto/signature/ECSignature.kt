package io.iohk.atala.prism.kotlin.crypto.signature

import io.iohk.atala.prism.kotlin.crypto.util.BytesOps.bytesToHex
import io.iohk.atala.prism.kotlin.crypto.util.BytesOps.hexToBytes

actual class ECSignature constructor(val sig: String) {
    actual constructor(data: List<UByte>) : this(bytesToHex(data))

    actual fun getEncoded(): List<Byte> =
        hexToBytes(sig).map { it.toByte() }

    actual fun getHexEncoded(): String =
        sig

    actual fun toDer(): List<Byte> =
        getEncoded()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class.js != other::class.js) return false

        other as ECSignature

        if (sig != other.sig) return false

        return true
    }

    override fun hashCode(): Int =
        sig.hashCode()
}
