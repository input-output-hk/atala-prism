package io.iohk.atala.prism.kotlin.identity

@JsExport
object DIDUrlCompanion {
    fun fromString(rawDidUrl: String): DIDUrl =
        DIDUrl.fromString(rawDidUrl)
}
