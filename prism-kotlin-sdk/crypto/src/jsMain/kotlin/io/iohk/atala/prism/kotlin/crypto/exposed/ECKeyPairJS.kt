package io.iohk.atala.prism.kotlin.crypto.exposed

import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.crypto.keys.ECKeyPair

@JsExport
data class ECKeyPairJS(
    val publicKey: ECPublicKeyJS,
    val privateKey: ECPrivateKeyJS
)

fun ECKeyPairJS.toEcKeyPair(): ECKeyPair =
    ECKeyPair(
        EC.toPublicKey(publicKey.getEncoded()),
        EC.toPrivateKey(privateKey.getEncoded()),
    )
