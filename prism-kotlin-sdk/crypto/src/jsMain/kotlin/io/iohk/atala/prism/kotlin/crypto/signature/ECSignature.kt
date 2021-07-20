package io.iohk.atala.prism.kotlin.crypto.signature

import io.iohk.atala.prism.kotlin.crypto.util.BytesOps.bytesToHex
import io.iohk.atala.prism.kotlin.crypto.util.BytesOps.hexToBytes

@JsExport
actual class ECSignature constructor(val sig: String) : ECSignatureCommon() {
    @JsName("fromBytes")
    actual constructor(data: ByteArray) : this(bytesToHex(data))

    override fun getEncoded(): ByteArray =
        hexToBytes(sig)
}
