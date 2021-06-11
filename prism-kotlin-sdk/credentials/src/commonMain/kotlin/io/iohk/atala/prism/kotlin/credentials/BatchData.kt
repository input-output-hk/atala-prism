package io.iohk.atala.prism.kotlin.credentials

import kotlin.js.JsExport

@JsExport
data class BatchData(
    val issuedOn: TimestampInfo,
    val revokedOn: TimestampInfo?
)
