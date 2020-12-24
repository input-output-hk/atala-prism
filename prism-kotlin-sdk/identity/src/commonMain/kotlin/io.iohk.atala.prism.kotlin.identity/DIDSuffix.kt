package io.iohk.atala.prism.kotlin.identity

import io.iohk.atala.prism.kotlin.crypto.SHA256Digest

data class DIDSuffix(val value: String) {
    companion object {
        private val suffixRegex = Regex("[:A-Za-z0-9_-]+$")

        fun fromString(suffix: String): DIDSuffix =
            apply(suffix)

        @ExperimentalUnsignedTypes
        fun fromDigest(digest: SHA256Digest): DIDSuffix =
            apply(digest.hexValue())

        private fun apply(didSuffix: String): DIDSuffix {
            require(suffixRegex.matches(didSuffix)) {
                "Invalid DID Suffix format: $didSuffix"
            }
            return DIDSuffix(didSuffix)
        }
    }

    override fun toString(): String = value
}
