package io.iohk.atala.prism.kotlin.credentials.exposed

import io.iohk.atala.prism.kotlin.credentials.KeyData
import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.crypto.util.BytesOps.hexToBytes

@JsExport
data class KeyDataJS(
    val publicKey: String,
    val addedOn: TimestampInfoJS,
    val revokedOn: TimestampInfoJS?
) {
    internal fun toKeyData(): KeyData =
        KeyData(EC.toPublicKey(hexToBytes(publicKey).map { it.toByte() }), addedOn.internal, revokedOn?.internal)
}
