package io.iohk.atala.prism.kotlin.extras

import io.iohk.atala.prism.kotlin.crypto.ECConfig
import io.iohk.atala.prism.kotlin.crypto.derivation.KeyType
import io.iohk.atala.prism.kotlin.crypto.derivation.KeyTypeEnum
import io.iohk.atala.prism.kotlin.crypto.keys.ECPublicKey
import io.iohk.atala.prism.kotlin.protos.ECKeyData
import io.iohk.atala.prism.kotlin.protos.KeyUsage
import io.iohk.atala.prism.kotlin.protos.PublicKey
import kotlin.js.JsExport

fun ECPublicKey.toECKeyData(): ECKeyData {
    val point = this.getCurvePoint()

    return ECKeyData(
        curve = ECConfig.CURVE_NAME,
        x = pbandk.ByteArr(point.x.bytes()),
        y = pbandk.ByteArr(point.y.bytes())
    )
}

@JsExport
fun KeyType.toKeyUsage(keyType: KeyTypeEnum) = KeyUsage.fromName(keyTypeToString(keyType))

@JsExport
fun ECKeyData.toPublicKey(
    id: String,
    keyUsage: KeyUsage
): PublicKey {
    return PublicKey(
        id = id,
        usage = keyUsage,
        keyData = PublicKey.KeyData.EcKeyData(this)
    )
}
