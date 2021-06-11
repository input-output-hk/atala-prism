package io.iohk.atala.prism.kotlin.credentials.exposed

import io.iohk.atala.prism.kotlin.credentials.json.JsonBasedCredential
import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.crypto.exposed.ECPrivateKeyJS
import io.iohk.atala.prism.kotlin.crypto.exposed.toKotlin
import io.iohk.atala.prism.kotlin.crypto.util.BytesOps.hexToBytes

@JsExport
object JsonBasedCredentialJSCompanion {
    fun fromString(credential: String): JsonBasedCredentialJS =
        JsonBasedCredentialJS(JsonBasedCredential.fromString(credential))
}

@JsExport
class JsonBasedCredentialJS internal constructor(
    internalJson: JsonBasedCredential
) : CredentialJS(internalJson) {
    @JsName("create")
    constructor(content: CredentialContentJS, signature: String? = null) :
        this(
            JsonBasedCredential(
                content.credentialContent,
                signature?.let { sig -> EC.toSignature(hexToBytes(sig)) }
            )
        )

    override val contentBytes = internalJson.contentBytes

    override val content: CredentialContentJS = CredentialContentJS(internalJson.content)

    override val signature: String? = internalJson.signature?.getHexEncoded()

    override val canonicalForm: String = internalJson.canonicalForm

    override fun sign(privateKey: ECPrivateKeyJS): CredentialJS =
        JsonBasedCredentialJS(credential.sign(privateKey.toKotlin()) as JsonBasedCredential)
}
