package io.iohk.atala.prism.kotlin.credentials

import io.iohk.atala.prism.kotlin.crypto.keys.ECPublicKey

data class KeyData(
    val publicKey: ECPublicKey,
    val addedOn: TimestampInfo,
    val revokedOn: TimestampInfo?
)
