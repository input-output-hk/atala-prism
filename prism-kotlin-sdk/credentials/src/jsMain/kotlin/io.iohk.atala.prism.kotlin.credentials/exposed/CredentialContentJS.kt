package io.iohk.atala.prism.kotlin.credentials.exposed

import io.iohk.atala.prism.kotlin.credentials.content.CredentialContent
import kotlinx.serialization.json.*

@JsExport
object CredentialContentJSCompanion {
    @JsName("fromString")
    fun fromString(value: String): CredentialContentJS =
        CredentialContentJS(CredentialContent.fromString(value))
}

@JsExport
data class CredentialContentJS internal constructor(internal val credentialContent: CredentialContent) {
    @JsName("getString")
    fun getString(field: String): String? =
        credentialContent.getString(field)

    @JsName("getInt")
    fun getInt(field: String): Int? =
        credentialContent.getInt(field)

    @JsName("getBoolean")
    fun getBoolean(field: String): Boolean? =
        credentialContent.getBoolean(field)

    @JsName("getArray")
    fun getArray(field: String): Array<String>? =
        credentialContent.getArray(field)?.map { it.toString() }?.toTypedArray()

    @JsName("getField")
    fun getField(fieldName: String): String? =
        credentialContent.getField(fieldName)?.toString()
}
