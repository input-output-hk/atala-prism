package io.iohk.atala.prism.kotlin.extras

import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.crypto.ECConfig
import io.iohk.atala.prism.kotlin.crypto.util.BytesOps.hexToBytes
import io.iohk.atala.prism.kotlin.protos.ECKeyData
import pbandk.ByteArr

@JsExport
fun toECKeyData(publicKeyHex: String): ECKeyData {
    val publicKey = EC.toPublicKey(hexToBytes(publicKeyHex).map { it.toByte() })
    val point = publicKey.getCurvePoint()

    return ECKeyData(
        curve = ECConfig.CURVE_NAME,
        x = ByteArr(point.x.toByteArray()),
        y = ByteArr(point.y.toByteArray())
    )
}
