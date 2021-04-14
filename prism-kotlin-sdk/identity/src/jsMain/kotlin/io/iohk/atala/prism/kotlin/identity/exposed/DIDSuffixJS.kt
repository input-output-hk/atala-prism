package io.iohk.atala.prism.kotlin.identity.exposed

import io.iohk.atala.prism.kotlin.crypto.SHA256Digest
import io.iohk.atala.prism.kotlin.identity.DIDSuffix

@JsExport
object DIDSuffixJSCompanion {
    @JsName("fromString")
    fun fromString(suffix: String): DIDSuffixJS =
        DIDSuffixJS(DIDSuffix.fromString(suffix))

    @JsName("fromDigest")
    fun fromDigest(hex: String): DIDSuffixJS =
        DIDSuffixJS(DIDSuffix.fromDigest(SHA256Digest.fromHex(hex)))
}

@JsExport
data class DIDSuffixJS internal constructor(private val didSuffix: DIDSuffix) {
    override fun toString(): String = didSuffix.toString()
}
