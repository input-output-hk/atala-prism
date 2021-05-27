package io.iohk.atala.prism.kotlin.identity

import io.iohk.atala.prism.kotlin.crypto.SHA256Digest
import io.iohk.atala.prism.kotlin.crypto.keys.ECPublicKey
import io.iohk.atala.prism.kotlin.identity.util.toProto
import io.iohk.atala.prism.kotlin.protos.*
import io.iohk.atala.prism.kotlin.protos.util.Base64Utils
import pbandk.encodeToByteArray
import kotlin.jvm.JvmStatic

class DID private constructor(val value: String) {
    companion object {
        val prismPrefix = "did:prism:"
        val prismRegex = Regex("^did:prism(:[A-Za-z0-9_-]+)+$")
        const val masterKeyId: String = "master0"

        // This is the prefix we currently use in IntDemo TODO: Remove once possible
        val testRegex = Regex("^did:test(:[A-Za-z0-9_-]+)+$")

        @JvmStatic
        fun buildPrismDID(stateHash: String, encodedState: String? = null): DID =
            if (encodedState == null) {
                DID("$prismPrefix$stateHash")
            } else {
                DID("$prismPrefix${buildSuffix(stateHash, encodedState)}")
            }

        @JvmStatic
        fun buildPrismDID(suffix: DIDSuffix): DID =
            DID("$prismPrefix${suffix.value}")

        @JvmStatic
        fun fromString(string: String): DID =
            if (prismRegex.matches(string) or testRegex.matches(string)) {
                DID(string)
            } else {
                throw IllegalArgumentException("Invalid DID: $string")
            }

        private fun buildSuffix(stateHash: String, encodedState: String): String =
            "$stateHash:$encodedState"

        @JvmStatic
        fun createUnpublishedDID(masterKey: ECPublicKey): DID {
            val createDidOp = CreateDIDOperation(
                didData = DIDData(
                    publicKeys = listOf(
                        PublicKey(
                            id = masterKeyId,
                            usage = KeyUsage.MASTER_KEY,
                            keyData = PublicKey.KeyData.EcKeyData(masterKey.toProto())
                        )
                    )
                )
            )

            val atalaOp = AtalaOperation(operation = AtalaOperation.Operation.CreateDid(createDidOp))
            val operationBytes = atalaOp.encodeToByteArray()
            val operationHash = SHA256Digest.compute(operationBytes.toList())
            val didCanonicalSuffix = operationHash.hexValue()
            val encodedOperation = Base64Utils.encode(operationBytes.toList())
            return buildPrismDID(didCanonicalSuffix, encodedOperation)
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
