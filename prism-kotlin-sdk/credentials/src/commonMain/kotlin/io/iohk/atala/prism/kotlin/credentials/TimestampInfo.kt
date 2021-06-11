package io.iohk.atala.prism.kotlin.credentials

import kotlin.js.JsExport

@JsExport
data class TimestampInfo(
    val atalaBlockTimestamp: Long, // timestamp provided from the underlying blockchain
    val atalaBlockSequenceNumber: Int, // transaction index inside the underlying blockchain block
    val operationSequenceNumber: Int // operation index inside the AtalaBlock
) {
    fun occurredBefore(later: TimestampInfo): Boolean {
        return (atalaBlockTimestamp < later.atalaBlockTimestamp) ||
            (
                atalaBlockTimestamp == later.atalaBlockTimestamp &&
                    atalaBlockSequenceNumber < later.atalaBlockSequenceNumber
                ) ||
            (
                atalaBlockTimestamp == later.atalaBlockTimestamp &&
                    atalaBlockSequenceNumber == later.atalaBlockSequenceNumber &&
                    operationSequenceNumber < later.operationSequenceNumber
                )
    }
}
