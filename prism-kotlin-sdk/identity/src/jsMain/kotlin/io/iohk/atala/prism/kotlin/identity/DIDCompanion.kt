package io.iohk.atala.prism.kotlin.identity

import io.iohk.atala.prism.kotlin.crypto.keys.ECPublicKey

@JsExport
object DIDCompanion {
    val prismPrefix = DID.prismPrefix
    const val masterKeyId: String = DID.masterKeyId

    fun buildPrismDID(stateHash: String, encodedState: String? = null): DID =
        DID.buildPrismDID(stateHash, encodedState)

    fun buildPrismDIDFromSuffix(suffix: DIDSuffix): DID =
        DID.buildPrismDID(suffix)

    fun fromString(string: String): DID =
        DID.fromString(string)

    fun createUnpublishedDID(masterKey: ECPublicKey, issuingKey: ECPublicKey? = null): DID =
        DID.createUnpublishedDID(masterKey, issuingKey)
}
