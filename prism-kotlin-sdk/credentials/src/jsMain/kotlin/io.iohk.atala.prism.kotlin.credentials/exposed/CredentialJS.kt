package io.iohk.atala.prism.kotlin.credentials.exposed

import io.iohk.atala.prism.kotlin.credentials.Credential
import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.crypto.exposed.ECPrivateKeyJS
import io.iohk.atala.prism.kotlin.crypto.exposed.SHA256DigestJS
import io.iohk.atala.prism.kotlin.crypto.exposed.toJs
import io.iohk.atala.prism.kotlin.crypto.util.BytesOps.hexToBytes

fun CredentialJS.toKotlin(): Credential =
    credential

@JsExport
abstract class CredentialJS internal constructor(internal val credential: Credential) {
    abstract val contentBytes: ByteArray

    abstract val content: CredentialContentJS

    abstract val signature: String?

    abstract val canonicalForm: String

    fun isVerifiable(): Boolean = credential.isVerifiable()

    fun hash(): SHA256DigestJS = credential.hash().toJs()

    abstract fun sign(privateKey: ECPrivateKeyJS): CredentialJS

    fun isValidSignature(publicKey: String): Boolean {
        val key = EC.toPublicKey(hexToBytes(publicKey).map { it.toByte() })
        return credential.isValidSignature(key)
    }
}
