package io.iohk.atala.prism.kotlin.credentials.exposed

import io.iohk.atala.prism.kotlin.credentials.content.CredentialContent
import io.iohk.atala.prism.kotlin.identity.exposed.DIDJS
import io.iohk.atala.prism.kotlin.identity.exposed.toJs

@JsExport
object CredentialContentJSCompanion {
    fun fromString(value: String): CredentialContentJS =
        CredentialContentJS(CredentialContent.fromString(value))
}

@JsExport
data class CredentialContentJS internal constructor(internal val credentialContent: CredentialContent) {
    fun getString(field: String): String? =
        credentialContent.getString(field)

    fun getInt(field: String): Int? =
        credentialContent.getInt(field)

    fun getBoolean(field: String): Boolean? =
        credentialContent.getBoolean(field)

    fun getArray(field: String): Array<String>? =
        credentialContent.getArray(field)?.map { it.toString() }?.toTypedArray()

    fun getField(fieldName: String): String? =
        credentialContent.getField(fieldName)?.toString()

    // Predefined fields
    fun getIssuerDid(): DIDJS? =
        credentialContent.getIssuerDid()?.toJs()

    fun getIssuanceKeyId(): String? =
        credentialContent.getIssuanceKeyId()

    fun getCredentialSubject(): String? =
        credentialContent.getCredentialSubject()
}
