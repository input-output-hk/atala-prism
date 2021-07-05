package io.iohk.atala.prism.kotlin.credentials.content

@JsExport
object CredentialContentCompanion {
    fun fromString(value: String): CredentialContent =
        CredentialContent.fromString(value)

    object JsonFields {
        sealed class Field(val field: String)
        object CredentialType : Field("type")
        object Issuer : Field("issuer")
        object IssuerDid : Field("id")
        object IssuerName : Field("name")
        object IssuanceKeyId : Field("keyId")
        object IssuanceDate : Field("issuanceDate")
        object ExpiryDate : Field("expiryDate")
        object CredentialSubject : Field("credentialSubject")
    }
}
