package io.iohk.atala.prism.kotlin.identity

import io.iohk.atala.prism.kotlin.crypto.SHA256Digest
import io.iohk.atala.prism.kotlin.crypto.derivation.KeyTypeEnum
import io.iohk.atala.prism.kotlin.crypto.derivation.MnemonicCode
import io.iohk.atala.prism.kotlin.crypto.keys.ECKeyPair
import io.iohk.atala.prism.kotlin.crypto.keys.ECPrivateKey
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

    fun createDIDFromMnemonic(mnemonic: MnemonicCode, didIndex: Int, passphrase: String = ""): CreateDIDContext =
        DID.createDIDFromMnemonic(mnemonic, didIndex, passphrase)

    fun deriveKeyFromFullPath(seed: ByteArray, didIndex: Int, keyType: KeyTypeEnum, keyIndex: Int): ECKeyPair =
        DID.deriveKeyFromFullPath(seed, didIndex, keyType, keyIndex)

    fun createUnpublishedDID(masterKey: ECPublicKey, issuingKey: ECPublicKey? = null): DID =
        DID.createUnpublishedDID(masterKey, issuingKey)

    fun updateDIDAtalaOperation(
        signingPrivateKey: ECPrivateKey,
        signingKeyId: String,
        did: DID,
        previousHash: SHA256Digest,
        keysToAdd: Array<KeyInformation> = emptyArray(),
        keysToRevoke: Array<String> = emptyArray()
    ): UpdateDIDContext =
        DID.updateDIDAtalaOperation(signingPrivateKey, signingKeyId, did, previousHash, keysToAdd.toList(), keysToRevoke.toList())
}
