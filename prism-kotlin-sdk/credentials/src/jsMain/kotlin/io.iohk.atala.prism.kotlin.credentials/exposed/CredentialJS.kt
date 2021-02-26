package io.iohk.atala.prism.kotlin.credentials.exposed

import io.iohk.atala.prism.kotlin.credentials.Credential
import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.crypto.util.BytesOps.hexToBytes

@JsExport
class CredentialJS internal constructor(internal val internal: Credential) {
    val contentBytes: ByteArray = internal.contentBytes.toByteArray()

    val content: CredentialContentJS = CredentialContentJS(internal.content)

    val signature: String? = internal.signature?.getHexEncoded()

    val canonicalForm: String = internal.canonicalForm

    fun isSigned(): Boolean = internal.isSigned()

    fun isUnverifiable(): Boolean = internal.isUnverifiable()

    fun hash(): String = internal.hash().hexValue()

    fun sign(privateKey: String): CredentialJS {
        val key = EC.toPrivateKey(hexToBytes(privateKey).map { it.toByte() })
        return CredentialJS(internal.sign(key))
    }

    fun isValidSignature(publicKey: String): Boolean {
        val key = EC.toPublicKey(hexToBytes(publicKey).map { it.toByte() })
        return internal.isValidSignature(key)
    }
}
