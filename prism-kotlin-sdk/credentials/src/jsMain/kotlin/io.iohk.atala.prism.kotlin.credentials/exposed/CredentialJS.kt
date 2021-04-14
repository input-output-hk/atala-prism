package io.iohk.atala.prism.kotlin.credentials.exposed

import io.iohk.atala.prism.kotlin.credentials.Credential
import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.crypto.util.BytesOps.hexToBytes

@JsExport
abstract class CredentialJS internal constructor(internal val credential: Credential) {
    abstract val contentBytes: ByteArray

    abstract val content: CredentialContentJS

    abstract val signature: String?

    abstract val canonicalForm: String

    @JsName("isSigned")
    fun isSigned(): Boolean = credential.isSigned()

    @JsName("isUnverifiable")
    fun isUnverifiable(): Boolean = credential.isUnverifiable()

    @JsName("hash")
    fun hash(): String = credential.hash().hexValue()

    @JsName("sign")
    abstract fun sign(privateKey: String): CredentialJS

    @JsName("isValidSignature")
    fun isValidSignature(publicKey: String): Boolean {
        val key = EC.toPublicKey(hexToBytes(publicKey).map { it.toByte() })
        return credential.isValidSignature(key)
    }
}
