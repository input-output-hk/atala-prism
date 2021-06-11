package io.iohk.atala.prism.kotlin.crypto.exposed

import io.iohk.atala.prism.kotlin.crypto.keys.ECPublicKey
import io.iohk.atala.prism.kotlin.crypto.util.BytesOps.bytesToHex

@JsExport
class ECPublicKeyJS internal constructor(internal val publicKey: ECPublicKey) {
    fun getHexEncoded(): String =
        publicKey.getHexEncoded()

    fun getEncoded(): ByteArray =
        publicKey.getEncoded()

    fun getCurvePoint(): ECPointJS {
        val curvePoint = publicKey.getCurvePoint()
        return ECPointJS(
            bytesToHex(curvePoint.x.toByteArray()),
            bytesToHex(curvePoint.y.toByteArray())
        )
    }
}

fun ECPublicKey.toJs(): ECPublicKeyJS =
    ECPublicKeyJS(this)

fun ECPublicKeyJS.toKotlin(): ECPublicKey =
    this.publicKey
