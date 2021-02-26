package io.iohk.atala.prism.kotlin.credentials.exposed

import io.iohk.atala.prism.kotlin.credentials.TimestampInfo

@JsExport
data class TimestampInfoJS internal constructor(internal val internal: TimestampInfo) {
    val atalaBlockTimestamp: String = internal.atalaBlockTimestamp.toString()
    val atalaBlockSequenceNumber: Int = internal.atalaBlockSequenceNumber
    val operationSequenceNumber: Int = internal.operationSequenceNumber

    fun occurredBefore(later: TimestampInfoJS): Boolean =
        internal.occurredBefore(later.internal)
}
