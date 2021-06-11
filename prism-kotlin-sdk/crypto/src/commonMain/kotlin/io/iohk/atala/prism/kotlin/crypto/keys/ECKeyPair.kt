package io.iohk.atala.prism.kotlin.crypto.keys

import kotlin.js.JsExport

@JsExport
data class ECKeyPair(val publicKey: ECPublicKey, val privateKey: ECPrivateKey)
