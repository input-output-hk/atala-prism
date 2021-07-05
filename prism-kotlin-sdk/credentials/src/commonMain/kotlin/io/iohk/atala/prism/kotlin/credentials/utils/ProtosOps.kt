package io.iohk.atala.prism.kotlin.credentials.utils

import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.protos.PublicKey
import io.iohk.atala.prism.kotlin.protos.TimestampInfo
import kotlinx.datetime.*
import kotlin.js.JsExport

@JsExport
fun PublicKey.toECPublicKey() =
    this.ecKeyData?.let {
        EC.toPublicKey(
            x = it.x.array,
            y = it.y.array
        )
    }

@JsExport
fun PublicKey.extractAddedOn() = this.addedOn?.toTimestampInfoModel()

@JsExport
fun PublicKey.extractRevokedOn() = this.revokedOn?.toTimestampInfoModel()

@JsExport
fun TimestampInfo.toTimestampInfoModel(): io.iohk.atala.prism.kotlin.credentials.TimestampInfo {
    val instant = Instant.fromEpochSeconds(blockTimestamp?.seconds!!, blockTimestamp?.nanos!!)
    return io.iohk.atala.prism.kotlin.credentials.TimestampInfo(
        atalaBlockTimestamp = instant.toEpochMilliseconds(),
        atalaBlockSequenceNumber = blockSequenceNumber,
        operationSequenceNumber = operationSequenceNumber
    )
}
