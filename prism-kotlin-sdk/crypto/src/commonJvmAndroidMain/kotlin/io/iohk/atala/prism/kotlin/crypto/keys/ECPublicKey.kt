package io.iohk.atala.prism.kotlin.crypto.keys

import io.iohk.atala.prism.kotlin.crypto.GenericJavaCryptography
import io.iohk.atala.prism.kotlin.crypto.util.toKotlinBigInteger
import java.security.PublicKey

actual class ECPublicKey(internal val key: PublicKey) : ECPublicKeyCommon() {
    override fun getCurvePoint(): ECPoint {
        val javaPoint = GenericJavaCryptography.publicKeyPoint(key)
        return ECPoint(javaPoint.affineX.toKotlinBigInteger(), javaPoint.affineY.toKotlinBigInteger())
    }
}
