package io.iohk.atala.prism.kotlin.identity.exposed

import io.iohk.atala.prism.kotlin.identity.*
import pbandk.encodeToByteArray

@JsExport
data class ValidatedLongFormJS(val stateHash: String, val encodedState: String, val initialState: ByteArray) {
    @JsName("suffix")
    fun suffix(): DIDSuffixJS = DIDSuffixJSCompanion.fromString("$stateHash:$encodedState")
}

@JsExport
sealed class DIDFormatJS

@JsExport
data class CanonicalJS(val suffix: String) : DIDFormatJS()

@JsExport
class LongFormJS internal constructor(private val longForm: LongForm) : DIDFormatJS() {
    @JsName("validate")
    fun validate(): ValidatedLongFormJS {
        val valid = longForm.validate()
        return ValidatedLongFormJS(valid.stateHash, valid.encodedState, valid.initialState.encodeToByteArray())
    }

    @JsName("getInitialState")
    fun getInitialState(): ByteArray {
        return validate().initialState
    }
}

@JsExport
object UnknownJS : DIDFormatJS()
