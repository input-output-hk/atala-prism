package io.iohk.atala.prism.kotlin.extras

import io.iohk.atala.prism.kotlin.credentials.KeyData
import io.iohk.atala.prism.kotlin.credentials.utils.extractAddedOn
import io.iohk.atala.prism.kotlin.credentials.utils.extractRevokedOn
import io.iohk.atala.prism.kotlin.credentials.utils.toECPublicKey
import io.iohk.atala.prism.kotlin.protos.DIDData
import io.iohk.atala.prism.kotlin.protos.PublicKey
import kotlin.js.JsExport

@JsExport
fun DIDData.findPublicKey(keyId: String): KeyData? {
    val didPublicKey: PublicKey? = publicKeys.find { it.id == keyId }
    return if (didPublicKey == null) {
        null
    } else {
        val publicKey = didPublicKey.toECPublicKey()!!
        KeyData(
            publicKey = publicKey,
            addedOn = didPublicKey.extractAddedOn()!!,
            revokedOn = didPublicKey.extractRevokedOn()
        )
    }
}
