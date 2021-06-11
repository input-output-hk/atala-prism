package io.iohk.atala.prism.kotlin.credentials.content

import io.iohk.atala.prism.kotlin.identity.DID
import kotlinx.serialization.json.*
import kotlin.js.JsExport

/**
 * Credential content JSON representation,
 * this class allows to access nested fields
 * with 'dot' notation like: credentialSubject.fieldName
 *
 * Example:
 * {{{
 *   import kotlinx.serialization.json.*
 *   val credentialContent = buildCredentialContent {
 *     put("issuerDid", "did")
 *     putJsonObject("credentialSubject") {
 *       put("fieldName", 123)
 *     }
 *   }
 *   credentialContent.getString("issuerDid") == "did"
 *   credentialContent.getInt("credentialSubject.fieldName") == 123
 * }}}
 */
@JsExport
data class CredentialContent(val fields: JsonObject) {

    fun getString(field: String): String? =
        getField(field)?.jsonPrimitive?.contentOrNull

    fun getInt(field: String): Int? =
        getField(field)?.jsonPrimitive?.int

    fun getBoolean(field: String): Boolean? =
        getField(field)?.jsonPrimitive?.boolean

    fun getArray(field: String): JsonArray? =
        getField(field)?.jsonArray

    /**
     * Get nested json primitives with 'dot' notation like: credentialSubject.fieldName
     */
    fun getField(fieldName: String): JsonElement? {
        val path = fieldName.split('.')
        return path.drop(1).fold(fields[path[0]]) { field, key ->
            field?.jsonObject?.get(key)
        }
    }

    // Predefined fields
    fun getIssuerDid(): DID? =
        getString(JsonField.IssuerDid.value)?.let {
            DID.fromString(it)
        }

    fun getIssuanceKeyId(): String? =
        getString(JsonField.IssuanceKeyId.value)

    fun getCredentialSubject(): String? =
        getString(JsonField.CredentialSubject.value)

    companion object {
        fun fromString(value: String): CredentialContent =
            CredentialContent(Json.parseToJsonElement(value).jsonObject)
    }
}

enum class JsonField(val value: String) {
    CredentialType("type"),
    Issuer("issuer"),
    IssuerDid("id"),
    IssuerName("name"),
    IssuanceKeyId("keyId"),
    IssuanceDate("issuanceDate"),
    ExpiryDate("expiryDate"),
    CredentialSubject("credentialSubject")
}

/**
 * Create a new CredentialContent instance using buildJsonObject syntax.
 */
inline fun buildCredentialContent(builderAction: JsonObjectBuilder.() -> Unit) =
    CredentialContent(buildJsonObject(builderAction))
