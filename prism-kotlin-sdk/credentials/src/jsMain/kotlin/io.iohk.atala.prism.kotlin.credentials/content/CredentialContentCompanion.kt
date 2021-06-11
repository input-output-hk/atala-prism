package io.iohk.atala.prism.kotlin.credentials.content

@JsExport
object CredentialContentCompanion {
    fun fromString(value: String): CredentialContent =
        CredentialContent.fromString(value)
}
