package io.iohk.atala.prism.kotlin.credentials

data class BatchData(
    val issuedOn: TimestampInfo,
    val revokedOn: TimestampInfo?
)
