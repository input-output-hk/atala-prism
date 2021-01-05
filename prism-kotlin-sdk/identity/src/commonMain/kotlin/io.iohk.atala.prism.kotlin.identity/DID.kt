package io.iohk.atala.prism.kotlin.identity

import io.iohk.atala.prism.kotlin.crypto.SHA256Digest
import io.iohk.atala.prism.kotlin.crypto.keys.ECPublicKey
import io.iohk.atala.prism.kotlin.identity.util.Base64Utils
import io.iohk.atala.prism.kotlin.identity.util.toProto
import io.iohk.atala.prism.protos.*
import pbandk.encodeToByteArray
import kotlin.jvm.JvmStatic

class DID private constructor(val value: String) {
    companion object {
        val prismPrefix = "did:prism:"
        val prismRegex = Regex("^did:prism(:[A-Za-z0-9_-]+)+$")

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

        @ExperimentalUnsignedTypes
        @JvmStatic
        fun createUnpublishedDID(masterKey: ECPublicKey): DID {
            val createDidOp = CreateDIDOperation(
                didData = DIDData(
                    publicKeys = listOf(
                        PublicKey(
                            id = "master0",
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
                DIDFormat.LongForm(
                    stripPrismPrefix().takeWhile { it != ':' },
                    stripPrismPrefix().dropWhile { it != ':' }.removePrefix(":")
                )
            }
            isCanonicalForm() -> DIDFormat.Canonical(stripPrismPrefix().takeWhile { it != ':' })
            else -> DIDFormat.Unknown
        }

    private fun stripPrismPrefix(): String = value.removePrefix(prismPrefix)

    // the method assumes that the DID is a PRISM DID
    val suffix: DIDSuffix
        get() = DIDSuffix.fromString(stripPrismPrefix())

    fun getCanonicalSuffix(): DIDSuffix? =
        when (val format = getFormat()) {
            is DIDFormat.Canonical -> DIDSuffix.fromString(format.suffix)
            is DIDFormat.LongForm -> DIDSuffix.fromString(format.stateHash)
            is DIDFormat.Unknown -> null
        }

    fun asLongForm(): DIDFormat.LongForm? =
        when (val format = getFormat()) {
            is DIDFormat.LongForm -> format
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
