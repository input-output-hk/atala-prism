package io.iohk.atala.prism.kotlin.credentials.exposed

import io.iohk.atala.prism.kotlin.credentials.KeyData
import io.iohk.atala.prism.kotlin.crypto.exposed.ECPublicKeyJS
import io.iohk.atala.prism.kotlin.crypto.exposed.toJs
import io.iohk.atala.prism.kotlin.crypto.exposed.toKotlin

fun KeyData.toJs(): KeyDataJS =
    KeyDataJS(
        publicKey.toJs(),
        addedOn.toJs(),
        revokedOn?.toJs()
    )

@JsExport
data class KeyDataJS(
    val publicKey: ECPublicKeyJS,
    val addedOn: TimestampInfoJS,
    val revokedOn: TimestampInfoJS?
) {
    internal fun toKeyData(): KeyData =
        KeyData(publicKey.toKotlin(), addedOn.internal, revokedOn?.internal)
}
