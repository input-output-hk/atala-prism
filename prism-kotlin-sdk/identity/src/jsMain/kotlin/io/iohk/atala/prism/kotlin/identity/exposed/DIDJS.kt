package io.iohk.atala.prism.kotlin.identity.exposed

import io.iohk.atala.prism.kotlin.crypto.exposed.ECPublicKeyJS
import io.iohk.atala.prism.kotlin.crypto.exposed.toKotlin
import io.iohk.atala.prism.kotlin.identity.*

fun DIDJS.toKotlin(): DID =
    this.did

fun DID.toJs(): DIDJS =
    DIDJS(this)

@JsExport
object DIDJSCompanion {
    val prismPrefix = DID.prismPrefix

    fun buildPrismDID(stateHash: String, encodedState: String? = null): DIDJS =
        DIDJS(DID.buildPrismDID(stateHash, encodedState))

    fun fromString(string: String): DIDJS =
        DIDJS(DID.fromString(string))

    fun createUnpublishedDID(masterKey: ECPublicKeyJS): DIDJS =
        DIDJS(DID.createUnpublishedDID(masterKey.toKotlin()))
}

@JsExport
class DIDJS internal constructor(internal val did: DID) {
    fun getValue(): String = did.value

    fun isLongForm(): Boolean = did.isLongForm()

    fun isCanonicalForm(): Boolean = did.isCanonicalForm()

    fun getFormat(): DIDFormatJS = when (val format = did.getFormat()) {
        is Canonical -> CanonicalJS(format.suffix)
        is LongForm -> LongFormJS(format)
        is Unknown -> UnknownJS
    }

    val suffix: DIDSuffixJS = DIDSuffixJS(did.suffix)

    fun getCanonicalSuffix(): DIDSuffixJS? {
        val suffix = did.getCanonicalSuffix()
        return if (suffix != null) {
            DIDSuffixJS(suffix)
        } else {
            null
        }
    }

    fun asLongForm(): LongFormJS? {
        val longForm = did.asLongForm()
        return if (longForm != null) {
            LongFormJS(longForm)
        } else {
            null
        }
    }
}
