package io.iohk.atala.prism.kotlin.extras

import io.iohk.atala.prism.kotlin.crypto.ECConfig
import io.iohk.atala.prism.kotlin.crypto.exposed.ECPublicKeyJS
import io.iohk.atala.prism.kotlin.crypto.exposed.toKotlin
import io.iohk.atala.prism.kotlin.protos.ECKeyData
import pbandk.ByteArr

@JsExport
fun ECPublicKeyJS.toECKeyData(): ECKeyData {
    val publicKey = this.toKotlin()
    val point = publicKey.getCurvePoint()

    return ECKeyData(
        curve = ECConfig.CURVE_NAME,
        x = ByteArr(point.x.toByteArray()),
        y = ByteArr(point.y.toByteArray())
    )
}
