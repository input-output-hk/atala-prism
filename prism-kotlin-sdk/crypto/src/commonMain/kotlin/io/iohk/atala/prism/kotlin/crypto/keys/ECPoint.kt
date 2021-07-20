package io.iohk.atala.prism.kotlin.crypto.keys

import com.ionspin.kotlin.bignum.integer.BigInteger
import kotlin.js.JsExport
import kotlin.js.JsName

@JsExport
data class ECPoint(val x: ECCoordinate, val y: ECCoordinate) {
    @JsName("fromBigIntegers")
    constructor(x: BigInteger, y: BigInteger) : this(ECCoordinate(x), ECCoordinate(y))
}
