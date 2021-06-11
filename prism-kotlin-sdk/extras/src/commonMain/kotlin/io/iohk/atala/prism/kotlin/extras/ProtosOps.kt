package io.iohk.atala.prism.kotlin.extras

import io.iohk.atala.prism.kotlin.credentials.KeyData
import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.protos.DIDData
import io.iohk.atala.prism.kotlin.protos.TimestampInfo
import kotlinx.datetime.*

fun DIDData.findPublicKey(keyId: String): KeyData? {
    val didPublicKey = publicKeys.find { it.id == keyId }
    return if (didPublicKey == null) {
        null
    } else {
        val publicKey = EC.toPublicKey(
            x = didPublicKey.ecKeyData?.x?.array!!,
            y = didPublicKey.ecKeyData?.y?.array!!
        )

        KeyData(
            publicKey = publicKey,
            addedOn = didPublicKey.addedOn!!.toTimestampInfoModel(),
            revokedOn = didPublicKey.revokedOn?.toTimestampInfoModel()
        )
    }
}

fun TimestampInfo.toTimestampInfoModel(): io.iohk.atala.prism.kotlin.credentials.TimestampInfo {
    val instant = Instant.fromEpochSeconds(blockTimestamp?.seconds!!, blockTimestamp?.nanos!!)
    return io.iohk.atala.prism.kotlin.credentials.TimestampInfo(
        atalaBlockTimestamp = instant.toEpochMilliseconds(),
        atalaBlockSequenceNumber = blockSequenceNumber,
        operationSequenceNumber = operationSequenceNumber
    )
}
