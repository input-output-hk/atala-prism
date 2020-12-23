package io.iohk.atala.prism.kotlin.identity

sealed class DIDFormatException(msg: String, cause: Throwable? = null) : Exception(msg, cause) {
    object CanonicalSuffixMatchStateException :
        DIDFormatException("Canonical suffix does not match the computed state")

    class InvalidAtalaOperationException(e: Exception) :
        DIDFormatException("Provided bytes do not encode a valid Atala Operation", e)
}
