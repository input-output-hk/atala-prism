package io.iohk.atala.prism.kotlin.identity.exposed

import io.iohk.atala.prism.kotlin.crypto.SHA256Digest
import io.iohk.atala.prism.kotlin.identity.DIDSuffix

fun DIDSuffixJS.toKotlin(): DIDSuffix =
    DIDSuffix.fromString(toString())

@JsExport
object DIDSuffixJSCompanion {
    fun fromString(suffix: String): DIDSuffixJS =
        DIDSuffixJS(DIDSuffix.fromString(suffix))

    fun fromDigest(hex: String): DIDSuffixJS =
        DIDSuffixJS(DIDSuffix.fromDigest(SHA256Digest.fromHex(hex)))
}

@JsExport
data class DIDSuffixJS internal constructor(private val didSuffix: DIDSuffix) {
    fun getValue(): String =
        didSuffix.value

    override fun toString(): String = didSuffix.toString()
}
