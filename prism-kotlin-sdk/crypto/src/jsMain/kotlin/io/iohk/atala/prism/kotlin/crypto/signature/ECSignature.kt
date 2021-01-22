package io.iohk.atala.prism.kotlin.crypto.signature

import io.iohk.atala.prism.kotlin.util.BytesOps.bytesToHex
import io.iohk.atala.prism.kotlin.util.BytesOps.hexToBytes

@ExperimentalUnsignedTypes
actual class ECSignature constructor(val sig: String) {
    actual constructor(data: List<UByte>) : this(bytesToHex(data))

    actual fun getEncoded(): List<Byte> =
        hexToBytes(sig).map { it.toByte() }

    actual fun getHexEncoded(): String =
        sig

    actual fun toDer(): List<Byte> =
        getEncoded()
}
