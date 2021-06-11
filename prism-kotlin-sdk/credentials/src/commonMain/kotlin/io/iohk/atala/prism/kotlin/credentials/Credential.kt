package io.iohk.atala.prism.kotlin.credentials

import io.iohk.atala.prism.kotlin.credentials.content.CredentialContent
import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.crypto.SHA256Digest
import io.iohk.atala.prism.kotlin.crypto.keys.ECPrivateKey
import io.iohk.atala.prism.kotlin.crypto.keys.ECPublicKey
import io.iohk.atala.prism.kotlin.crypto.signature.ECSignature
import kotlin.js.JsExport

@JsExport
abstract class Credential {
    abstract val contentBytes: ByteArray

    abstract val content: CredentialContent

    abstract val signature: ECSignature?

    abstract val canonicalForm: String

    fun isVerifiable(): Boolean = signature != null

    fun hash(): SHA256Digest = SHA256Digest.compute(canonicalForm.encodeToByteArray())

    abstract fun sign(privateKey: ECPrivateKey): Credential

    fun isValidSignature(publicKey: ECPublicKey): Boolean {
        return signature?.let { EC.verify(contentBytes, publicKey, it) } ?: false
    }
}
