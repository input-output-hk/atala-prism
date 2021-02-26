package io.iohk.atala.prism.kotlin.credentials.exposed

import io.iohk.atala.prism.kotlin.credentials.CredentialData

@JsExport
data class CredentialDataJS(
    val issuedOn: TimestampInfoJS,
    val revokedOn: TimestampInfoJS?
) {
    internal fun toCredentialData(): CredentialData =
        CredentialData(issuedOn.internal, revokedOn?.internal)
}
