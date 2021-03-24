package io.iohk.atala.prism.kotlin.credentials.json

import io.iohk.atala.prism.kotlin.credentials.Credential
import io.iohk.atala.prism.kotlin.credentials.CredentialParsingError
import io.iohk.atala.prism.kotlin.credentials.content.CredentialContent
import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.crypto.keys.ECPrivateKey
import io.iohk.atala.prism.kotlin.crypto.signature.ECSignature
import io.iohk.atala.prism.kotlin.protos.util.Base64Utils

data class JsonBasedCredential constructor(
    override val content: CredentialContent,
    override val signature: ECSignature? = null
) : Credential() {

    override val contentBytes = content.fields.toString().encodeToByteArray().toList()

    override val canonicalForm: String =
        if (signature != null) {
            Base64Utils.encode(contentBytes.toByteArray().toList()) +
                SEPARATOR +
                Base64Utils.encode(signature.getEncoded())
        } else {
            contentBytes.toByteArray().decodeToString()
        }

    override fun sign(privateKey: ECPrivateKey): Credential {
        return copy(signature = EC.sign(contentBytes, privateKey))
    }

    companion object {
        val SEPARATOR = '.'

        private fun parseUnsignedCredential(credential: String): JsonBasedCredential {
            val credentialContent = CredentialContent.fromString(credential)
            return JsonBasedCredential(credentialContent)
        }

        private fun parseSignedCredential(credential: String): JsonBasedCredential {
            val contentWithSignature = credential.split(SEPARATOR)
            if (contentWithSignature.size == 2) {
                val content = contentWithSignature[0]
                val signature = contentWithSignature[1]

                try {
                    val credentialContent =
                        CredentialContent.fromString(Base64Utils.decode(content).toByteArray().decodeToString())
                    return JsonBasedCredential(
                        content = credentialContent,
                        signature = ECSignature(Base64Utils.decode(signature).map { it.toUByte() })
                    )
                } catch (e: Exception) {
                    throw CredentialParsingError("Failed to parse signed credential content: ${e.message}")
                }
            } else {
                throw CredentialParsingError(
                    "Failed to parse signed credential. " +
                        "Expected format: [encoded credential]$SEPARATOR[encoded signature]"
                )
            }
        }

        fun fromString(credential: String): JsonBasedCredential {
            try {
                return parseUnsignedCredential(credential)
            } catch (e: Exception) {
                return parseSignedCredential(credential)
            }
        }
    }
}
