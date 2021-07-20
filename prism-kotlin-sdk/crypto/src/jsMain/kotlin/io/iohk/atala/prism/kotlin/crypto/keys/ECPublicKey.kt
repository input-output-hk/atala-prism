package io.iohk.atala.prism.kotlin.crypto.keys

import com.ionspin.kotlin.bignum.integer.BigInteger
import io.iohk.atala.prism.kotlin.crypto.externals.base

@JsExport
actual class ECPublicKey(val basePoint: base.BasePoint) : ECPublicKeyCommon() {
    override fun getCurvePoint(): ECPoint {
        val x = BigInteger.parseString(basePoint.getX().toString())
        val y = BigInteger.parseString(basePoint.getY().toString())
        return ECPoint(x, y)
    }
}
