package io.iohk.atala.prism.kotlin.crypto

import com.ionspin.kotlin.bignum.integer.BigInteger
import io.iohk.atala.prism.kotlin.crypto.keys.ECPoint
import kotlin.js.JsExport

@JsExport
object Secp256k1 {
    fun isSecp256k1(point: ECPoint): Boolean {
        val x = point.x.coordinate
        val y = point.y.coordinate

        return ((y * y - x * x * x - ECConfig.b) mod ECConfig.p) == BigInteger.ZERO
    }
}
