package io.iohk.atala.prism.kotlin.crypto.exposed

@ExperimentalJsExport
@JsExport
data class ECKeyPairJS(val publicKey: String, val privateKey: String)
