package io.iohk.atala.prism.kotlin.credentials

data class CredentialData(
    val issuedOn: TimestampInfo,
    val revokedOn: TimestampInfo?
)
