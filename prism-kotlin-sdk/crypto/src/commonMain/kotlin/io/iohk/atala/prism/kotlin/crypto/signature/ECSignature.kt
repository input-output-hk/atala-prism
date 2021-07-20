package io.iohk.atala.prism.kotlin.crypto.signature

import io.iohk.atala.prism.kotlin.crypto.Encodable
import kotlin.js.JsExport

@JsExport
abstract class ECSignatureCommon : Encodable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ECSignatureCommon

        if (!getEncoded().contentEquals(other.getEncoded())) return false

        return true
    }

    override fun hashCode(): Int =
        getEncoded().hashCode()
}

expect class ECSignature(data: ByteArray) : ECSignatureCommon
