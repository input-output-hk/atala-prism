package io.iohk.atala.prism.kotlin.identity

import io.iohk.atala.prism.kotlin.crypto.SHA256Digest
import io.iohk.atala.prism.kotlin.identity.util.Base64Utils
import io.iohk.atala.prism.kotlin.protos.AtalaOperation
import pbandk.decodeFromByteArray
import kotlin.js.JsExport

data class ValidatedLongForm(val stateHash: String, val encodedState: String, val initialState: AtalaOperation) {
    fun suffix(): DIDSuffix = DIDSuffix.fromString("$stateHash:$encodedState")
}

@JsExport
sealed class DIDFormat {
    companion object {
        val longFormRegex = Regex("^did:prism:[0-9a-f]{64}:[A-Za-z0-9_-]+[=]*$")
        val shortFormRegex = Regex("^did:prism:[0-9a-f]{64}$")
    }

    data class Canonical(val suffix: String) : DIDFormat()
    data class LongForm(val stateHash: String, val encodedState: String) : DIDFormat() {
        fun validate(): ValidatedLongForm {
            val atalaOperationBytes = Base64Utils.decode(encodedState)
            if (stateHash == SHA256Digest.compute(atalaOperationBytes).hexValue()) {
                val operation = try {
                    AtalaOperation.decodeFromByteArray(atalaOperationBytes.toByteArray())
                } catch (e: Exception) {
                    throw DIDFormatException.InvalidAtalaOperationException(e)
                }
                return ValidatedLongForm(stateHash, encodedState, operation)
            } else {
                throw DIDFormatException.CanonicalSuffixMatchStateException
            }
        }

        fun getInitialState(): AtalaOperation {
            return validate().initialState
        }
    }
    object Unknown : DIDFormat()
}
