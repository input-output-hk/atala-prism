package io.iohk.atala.prism.kotlin.identity.exposed

import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.crypto.util.BytesOps
import io.iohk.atala.prism.kotlin.identity.*

fun DIDJS.toKotlin(): DID =
    this.did

@JsExport
object DIDJSCompanion {
    val prismPrefix = DID.prismPrefix

    @JsName("buildPrismDID")
    fun buildPrismDID(stateHash: String, encodedState: String? = null): DIDJS =
        DIDJS(DID.buildPrismDID(stateHash, encodedState))

    @JsName("fromString")
    fun fromString(string: String): DIDJS =
        DIDJS(DID.fromString(string))

    @JsName("createUnpublishedDID")
    fun createUnpublishedDID(masterKey: String): DIDJS {
        val key = EC.toPublicKey(BytesOps.hexToBytes(masterKey).map { it.toByte() })
        return DIDJS(DID.createUnpublishedDID(key))
    }
}

@JsExport
class DIDJS internal constructor(internal val did: DID) {
    @JsName("getValue")
    fun getValue(): String = did.value

    @JsName("isLongForm")
    fun isLongForm(): Boolean = did.isLongForm()

    @JsName("isCanonicalForm")
    fun isCanonicalForm(): Boolean = did.isCanonicalForm()

    @JsName("getFormat")
    fun getFormat(): DIDFormatJS = when (val format = did.getFormat()) {
        is Canonical -> CanonicalJS(format.suffix)
        is LongForm -> LongFormJS(format)
        is Unknown -> UnknownJS
    }

    @JsName("suffix")
    val suffix: DIDSuffixJS = DIDSuffixJS(did.suffix)

    @JsName("getCanonicalSuffix")
    fun getCanonicalSuffix(): DIDSuffixJS? {
        val suffix = did.getCanonicalSuffix()
        return if (suffix != null) {
            DIDSuffixJS(suffix)
        } else {
            null
        }
    }

    @JsName("asLongForm")
    fun asLongForm(): LongFormJS? {
        val longForm = did.asLongForm()
        return if (longForm != null) {
            LongFormJS(longForm)
        } else {
            null
        }
    }
}
