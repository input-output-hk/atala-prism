package io.iohk.atala.prism.kotlin.crypto.keys

import com.ionspin.kotlin.bignum.integer.BigInteger
import kotlin.js.JsExport

@JsExport
data class ECPoint(val x: BigInteger, val y: BigInteger) {
    fun xBytes(): ByteArray = x.toByteArray()

    fun yBytes(): ByteArray = y.toByteArray()
}
