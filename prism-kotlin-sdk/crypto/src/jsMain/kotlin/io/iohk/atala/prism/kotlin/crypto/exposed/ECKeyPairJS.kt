package io.iohk.atala.prism.kotlin.crypto.exposed

import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.crypto.keys.ECKeyPair
import io.iohk.atala.prism.kotlin.crypto.util.BytesOps

@JsExport
data class ECKeyPairJS(
    @JsName("publicKey") val publicKey: String,
    @JsName("privateKey") val privateKey: String
)

fun ECKeyPairJS.toEcKeyPair(): ECKeyPair =
    ECKeyPair(
        EC.toPublicKey(BytesOps.hexToBytes(publicKey).map { it.toByte() }),
        EC.toPrivateKey(BytesOps.hexToBytes(privateKey).map { it.toByte() }),
    )
