package io.iohk.atala.prism.kotlin.identity

import io.iohk.atala.prism.kotlin.crypto.SHA256Digest
import io.iohk.atala.prism.kotlin.crypto.derivation.*
import io.iohk.atala.prism.kotlin.crypto.derivation.KeyDerivation.derivationRoot
import io.iohk.atala.prism.kotlin.crypto.keys.ECKeyPair
import io.iohk.atala.prism.kotlin.crypto.keys.ECPrivateKey
import io.iohk.atala.prism.kotlin.crypto.keys.ECPublicKey
import io.iohk.atala.prism.kotlin.identity.util.ECProtoOps
import io.iohk.atala.prism.kotlin.identity.util.toProto
import io.iohk.atala.prism.kotlin.protos.*
import io.iohk.atala.prism.kotlin.protos.util.Base64Utils
import pbandk.encodeToByteArray
import kotlin.js.JsExport
import kotlin.js.JsName
import kotlin.jvm.JvmStatic

@JsExport
class DID private constructor(val value: String) {
    companion object {
        val prismPrefix = "did:prism:"
        val prismRegex = Regex("^did:prism(:[A-Za-z0-9_-]+)+$")
        const val masterKeyId: String = "master0"
        const val issuingKeyId: String = "issuing0"

        // This is the prefix we currently use in IntDemo TODO: Remove once possible
        val testRegex = Regex("^did:test(:[A-Za-z0-9_-]+)+$")

        private data class AtalaOperationWithHash(val atalaOperation: AtalaOperation, val atalaOperationHash: SHA256Digest)

        @JvmStatic
        fun buildPrismDID(stateHash: String, encodedState: String? = null): DID =
            if (encodedState == null) {
                DID("$prismPrefix$stateHash")
            } else {
                DID("$prismPrefix${buildSuffix(stateHash, encodedState)}")
            }

        @JvmStatic
        @JsName("buildPrismDIDFromSuffix")
        fun buildPrismDID(suffix: DIDSuffix): DID =
            DID("$prismPrefix${suffix.value}")

        @JvmStatic
        fun fromString(string: String): DID =
            if (prismRegex.matches(string) or testRegex.matches(string)) {
                DID(string)
            } else {
                throw IllegalArgumentException("Invalid DID: $string")
            }

        @JvmStatic
        fun deriveKeyFromFullPath(seed: ByteArray, didIndex: Int, keyType: KeyTypeEnum, keyIndex: Int): ECKeyPair =
            derivationRoot(seed)
                .derive(DerivationAxis.hardened(didIndex))
                .derive(DerivationAxis.hardened(keyType))
                .derive(DerivationAxis.hardened(keyIndex))
                .keyPair()

        private fun buildSuffix(stateHash: String, encodedState: String): String =
            "$stateHash:$encodedState"

        @JvmStatic
        fun createDIDFromMnemonic(mnemonic: MnemonicCode, didIndex: Int, passphrase: String = ""): CreateDIDContext {
            val seed = KeyDerivation.binarySeed(mnemonic, passphrase)
            val masterECKeyPair = deriveKeyFromFullPath(seed, didIndex, KeyType.MASTER_KEY, 0)
            val did = createUnpublishedDID(masterECKeyPair.publicKey)
            val (atalaOp, operationHash) = createDIDAtalaOperation(masterECKeyPair.publicKey)
            return CreateDIDContext(
                unpublishedDID = did,
                createDIDSignedOperation = ECProtoOps.signedAtalaOperation(masterECKeyPair.privateKey, masterKeyId, atalaOp),
                operationHash = operationHash
            )
        }

        @JvmStatic
        fun createUnpublishedDID(masterKey: ECPublicKey, issuingKey: ECPublicKey? = null): DID {
            val (atalaOp, operationHash) = createDIDAtalaOperation(masterKey, issuingKey)
            val didCanonicalSuffix = operationHash.hexValue()
            val encodedOperation = Base64Utils.encode(atalaOp.encodeToByteArray())
            return buildPrismDID(didCanonicalSuffix, encodedOperation)
        }

        @JvmStatic
        private fun createDIDAtalaOperation(masterKey: ECPublicKey, issuingKey: ECPublicKey? = null): AtalaOperationWithHash {
            val masterKeyPublicKey =
                listOf(
                    PublicKey(
                        id = masterKeyId,
                        usage = KeyUsage.MASTER_KEY,
                        keyData = PublicKey.KeyData.EcKeyData(masterKey.toProto())
                    )
                )
            val issuingKeyPublicKey =
                issuingKey?.let {
                    listOf(
                        PublicKey(
                            id = issuingKeyId,
                            usage = KeyUsage.ISSUING_KEY,
                            keyData = PublicKey.KeyData.EcKeyData(it.toProto())
                        )
                    )
                }.orEmpty()
            val createDidOp = CreateDIDOperation(
                didData = DIDData(
                    publicKeys = masterKeyPublicKey + issuingKeyPublicKey
                )
            )
            val atalaOp = AtalaOperation(operation = AtalaOperation.Operation.CreateDid(createDidOp))
            val operationBytes = atalaOp.encodeToByteArray()
            val operationHash = SHA256Digest.compute(operationBytes)
            return AtalaOperationWithHash(atalaOp, operationHash)
        }

        @JvmStatic
        fun updateDIDAtalaOperation(
            signingPrivateKey: ECPrivateKey,
            signingKeyId: String,
            did: DID,
            previousHash: SHA256Digest,
            keysToAdd: List<KeyInformation> = emptyList(),
            keysToRevoke: List<String> = emptyList()
        ): UpdateDIDContext {
            require(did.isCanonicalForm()) {
                "DID should be in canonical form, found DID: $did"
            }

            val atalaUpdateOperation = AtalaOperation(
                operation = AtalaOperation.Operation.UpdateDid(
                    updateDid = UpdateDIDOperation(
                        previousOperationHash = pbandk.ByteArr(previousHash.value),
                        id = did.suffix.value,
                        actions = keysToAdd.map { keyInformation ->
                            UpdateDIDAction(
                                UpdateDIDAction.Action.AddKey(
                                    AddKeyAction(keyInformation.toPublicKey())
                                )
                            )
                        } + keysToRevoke.map { keyToRevoke ->
                            UpdateDIDAction(
                                UpdateDIDAction.Action.RemoveKey(
                                    RemoveKeyAction(keyToRevoke)
                                )
                            )
                        }
                    )
                )
            )

            return UpdateDIDContext(
                operationHash = SHA256Digest.compute(atalaUpdateOperation.encodeToByteArray()),
                updateDIDSignedOperation = ECProtoOps.signedAtalaOperation(
                    privateKey = signingPrivateKey,
                    signedWith = signingKeyId,
                    atalaOperation = atalaUpdateOperation
                )
            )
        }
    }

    fun isLongForm(): Boolean = DIDFormat.longFormRegex.matches(value)

    fun isCanonicalForm(): Boolean = DIDFormat.shortFormRegex.matches(value)

    fun getFormat(): DIDFormat =
        when {
            isLongForm() -> {
                LongForm(
                    suffix.value.takeWhile { it != ':' },
                    suffix.value.dropWhile { it != ':' }.removePrefix(":")
                )
            }
            isCanonicalForm() -> Canonical(suffix.value.takeWhile { it != ':' })
            else -> Unknown
        }

    // the method assumes that the DID is a PRISM DID
    val suffix: DIDSuffix
        get() = DIDSuffix.fromString(value.removePrefix(prismPrefix))

    fun getCanonicalSuffix(): DIDSuffix? =
        when (val format = getFormat()) {
            is Canonical -> DIDSuffix.fromString(format.suffix)
            is LongForm -> DIDSuffix.fromString(format.stateHash)
            is Unknown -> null
        }

    fun asLongForm(): LongForm? =
        when (val format = getFormat()) {
            is LongForm -> format
            else -> null
        }

    override fun toString(): String = value

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is DID -> value == other.value
            else -> false
        }
    }

    override fun hashCode(): Int = value.hashCode()
}

@JsExport
data class KeyInformation(
    val keyId: String,
    val keyTypeEnum: KeyTypeEnum,
    val publicKey: ECPublicKey
) {
    fun toPublicKey(): PublicKey = PublicKey(
        id = keyId,
        usage = KeyUsage.fromName(KeyType.keyTypeToString(keyTypeEnum)),
        keyData = publicKey.toProto().let { PublicKey.KeyData.EcKeyData(it) }
    )
}
