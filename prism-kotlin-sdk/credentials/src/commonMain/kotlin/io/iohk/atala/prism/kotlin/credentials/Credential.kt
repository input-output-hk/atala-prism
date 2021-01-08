package io.iohk.atala.prism.kotlin.credentials

import io.iohk.atala.prism.kotlin.credentials.content.CredentialContent
import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.crypto.SHA256Digest
import io.iohk.atala.prism.kotlin.crypto.keys.ECPrivateKey
import io.iohk.atala.prism.kotlin.crypto.keys.ECPublicKey
import io.iohk.atala.prism.kotlin.crypto.signature.ECSignature

abstract class Credential {
    abstract val contentBytes: List<Byte>

    abstract val content: CredentialContent

    @ExperimentalUnsignedTypes
    abstract val signature: ECSignature?

    abstract val canonicalForm: String

    @ExperimentalUnsignedTypes
    fun isSigned(): Boolean = signature != null

    @ExperimentalUnsignedTypes
    fun isUnverifiable(): Boolean = signature == null

    @ExperimentalUnsignedTypes
    fun hash(): SHA256Digest = SHA256Digest.compute(canonicalForm.encodeToByteArray().toList())

    abstract fun sign(privateKey: ECPrivateKey): Credential

    @ExperimentalUnsignedTypes
    fun isValidSignature(publicKey: ECPublicKey): Boolean {
        return signature?.let { EC.verify(contentBytes, publicKey, it) } ?: false
    }
}
