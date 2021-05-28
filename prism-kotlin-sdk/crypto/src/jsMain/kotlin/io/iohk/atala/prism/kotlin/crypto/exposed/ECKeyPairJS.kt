package io.iohk.atala.prism.kotlin.crypto.exposed

import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.crypto.keys.ECKeyPair
import io.iohk.atala.prism.kotlin.crypto.util.BytesOps

@JsExport
data class ECKeyPairJS(
    val publicKey: String,
    val privateKey: String
)

fun ECKeyPairJS.toEcKeyPair(): ECKeyPair =
    ECKeyPair(
        EC.toPublicKey(BytesOps.hexToBytes(publicKey).map { it.toByte() }),
        EC.toPrivateKey(BytesOps.hexToBytes(privateKey).map { it.toByte() }),
    )
