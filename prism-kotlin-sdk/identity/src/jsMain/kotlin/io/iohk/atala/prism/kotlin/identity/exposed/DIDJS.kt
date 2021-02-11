package io.iohk.atala.prism.kotlin.identity.exposed

import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.crypto.util.BytesOps
import io.iohk.atala.prism.kotlin.identity.DID
import io.iohk.atala.prism.kotlin.identity.DIDFormat
import io.iohk.atala.prism.kotlin.identity.DIDSuffix

@JsExport
class DIDJS private constructor(private val did: DID) {
    companion object {
        val prismPrefix = DID.prismPrefix

        fun buildPrismDID(stateHash: String, encodedState: String? = null): DIDJS =
            DIDJS(DID.buildPrismDID(stateHash, encodedState))

        fun fromString(string: String): DIDJS =
            DIDJS(DID.fromString(string))

        fun createUnpublishedDID(masterKey: String): DIDJS {
            val key = EC.toPublicKey(BytesOps.hexToBytes(masterKey).map { it.toByte() })
            return DIDJS(DID.createUnpublishedDID(key))
        }
    }

    fun isLongForm(): Boolean = did.isLongForm()

    fun isCanonicalForm(): Boolean = did.isCanonicalForm()

    fun getFormat(): DIDFormat = did.getFormat()

    fun stripPrismPrefix(): String = did.stripPrismPrefix()

    val suffix: DIDSuffix = did.suffix

    fun getCanonicalSuffix(): DIDSuffix? = did.getCanonicalSuffix()

    fun asLongForm(): DIDFormat.LongForm? = did.asLongForm()
}
