package io.iohk.atala.prism.kotlin.identity.util

import io.iohk.atala.prism.kotlin.crypto.ECConfig
import io.iohk.atala.prism.kotlin.crypto.keys.ECPublicKey
import io.iohk.atala.prism.kotlin.protos.ECKeyData
import pbandk.ByteArr

fun ECPublicKey.toProto(): ECKeyData {
    val point = getCurvePoint()
    return ECKeyData(
        curve = ECConfig.CURVE_NAME,
        x = ByteArr(point.x.toByteArray()),
        y = ByteArr(point.y.toByteArray())
    )
}
