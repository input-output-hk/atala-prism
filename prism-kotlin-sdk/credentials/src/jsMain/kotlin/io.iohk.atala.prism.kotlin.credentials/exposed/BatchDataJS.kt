package io.iohk.atala.prism.kotlin.credentials.exposed

import io.iohk.atala.prism.kotlin.credentials.BatchData

@JsExport
data class BatchDataJS(
    val issuedOn: TimestampInfoJS,
    val revokedOn: TimestampInfoJS?
) {
    internal fun toBatchData(): BatchData =
        BatchData(issuedOn.internal, revokedOn?.internal)
}
