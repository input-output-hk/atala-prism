package io.iohk.atala.prism.kotlin.identity

import io.iohk.atala.prism.kotlin.crypto.SHA256Digest

@JsExport
object DIDSuffixCompanion {
    fun fromString(suffix: String): DIDSuffix =
        DIDSuffix.fromString(suffix)

    fun fromDigest(digest: SHA256Digest): DIDSuffix =
        DIDSuffix.fromDigest(digest)
}
