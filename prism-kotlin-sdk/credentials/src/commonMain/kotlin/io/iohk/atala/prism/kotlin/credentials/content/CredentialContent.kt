package io.iohk.atala.prism.kotlin.credentials.content

import kotlinx.serialization.json.*

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

    companion object {
        fun fromString(value: String): CredentialContent =
            CredentialContent(Json.parseToJsonElement(value).jsonObject)
    }
}

/**
 * Create a new CredentialContent instance using buildJsonObject syntax.
 */
inline fun buildCredentialContent(builderAction: JsonObjectBuilder.() -> Unit) =
    CredentialContent(buildJsonObject(builderAction))
