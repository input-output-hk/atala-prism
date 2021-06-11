package io.iohk.atala.prism.kotlin.credentials.json

@JsExport
object JsonBasedCredentialCompanion {
    fun fromString(credential: String): JsonBasedCredential =
        JsonBasedCredential.fromString(credential)
}
