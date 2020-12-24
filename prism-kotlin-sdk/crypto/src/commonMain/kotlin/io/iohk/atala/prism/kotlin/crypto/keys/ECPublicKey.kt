package io.iohk.atala.prism.kotlin.crypto.keys

expect class ECPublicKey : ECKey {
    fun getCurvePoint(): ECPoint
}
