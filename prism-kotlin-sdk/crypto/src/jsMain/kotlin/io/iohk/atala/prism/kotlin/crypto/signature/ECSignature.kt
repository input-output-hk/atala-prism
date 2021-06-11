package io.iohk.atala.prism.kotlin.crypto.signature

import io.iohk.atala.prism.kotlin.crypto.util.BytesOps.bytesToHex
import io.iohk.atala.prism.kotlin.crypto.util.BytesOps.hexToBytes

@JsExport
actual class ECSignature constructor(val sig: String) : ECSignatureCommon() {
    @JsName("fromBytes")
    actual constructor(data: ByteArray) : this(bytesToHex(data))

    override fun getEncoded(): ByteArray =
        hexToBytes(sig)

    override fun toDer(): ByteArray =
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
