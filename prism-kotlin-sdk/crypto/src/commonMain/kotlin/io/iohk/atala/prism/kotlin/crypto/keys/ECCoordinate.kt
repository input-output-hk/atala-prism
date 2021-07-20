package io.iohk.atala.prism.kotlin.crypto.keys

import com.ionspin.kotlin.bignum.integer.BigInteger
import io.iohk.atala.prism.kotlin.crypto.ECConfig
import io.iohk.atala.prism.kotlin.crypto.util.padStart
import kotlin.js.JsExport

@JsExport
data class ECCoordinate(val coordinate: BigInteger) {
    fun bytes(): ByteArray = coordinate.toByteArray().padStart(ECConfig.PRIVATE_KEY_BYTE_SIZE, 0)
}
