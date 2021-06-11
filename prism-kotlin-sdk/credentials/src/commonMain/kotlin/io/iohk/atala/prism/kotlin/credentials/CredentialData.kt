package io.iohk.atala.prism.kotlin.credentials

import kotlin.js.JsExport

@JsExport
data class CredentialData(
    val issuedOn: TimestampInfo,
    val revokedOn: TimestampInfo?
)
