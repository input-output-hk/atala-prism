package io.iohk.atala.prism.kotlin.credentials.exposed

import io.iohk.atala.prism.kotlin.credentials.TimestampInfo

fun TimestampInfo.toJs(): TimestampInfoJS =
    TimestampInfoJS(this)

@JsExport
class TimestampInfoJS internal constructor(internal val internal: TimestampInfo) {
    @JsName("create")
    constructor(
        atalaBlockTimestamp: String,
        atalaBlockSequenceNumber: Int,
        operationSequenceNumber: Int
    ) : this(TimestampInfo(atalaBlockTimestamp.toLong(), atalaBlockSequenceNumber, operationSequenceNumber))

    val atalaBlockTimestamp: String = internal.atalaBlockTimestamp.toString()
    val atalaBlockSequenceNumber: Int = internal.atalaBlockSequenceNumber
    val operationSequenceNumber: Int = internal.operationSequenceNumber

    fun occurredBefore(later: TimestampInfoJS): Boolean =
        internal.occurredBefore(later.internal)
}
