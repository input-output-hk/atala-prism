package io.iohk.atala.prism.kotlin.crypto.keys

import io.iohk.atala.prism.kotlin.crypto.Encodable
import kotlin.js.JsExport

@JsExport
abstract class ECKey : Encodable {
    override fun hashCode(): Int {
        return getEncoded().hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is ECKey -> getEncoded().contentEquals(other.getEncoded())
            else -> false
        }
    }
}
