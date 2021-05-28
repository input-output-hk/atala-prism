package io.iohk.atala.prism.kotlin.extras

import io.iohk.atala.prism.kotlin.credentials.exposed.KeyDataJS
import io.iohk.atala.prism.kotlin.credentials.exposed.TimestampInfoJS
import io.iohk.atala.prism.kotlin.credentials.exposed.toJs
import io.iohk.atala.prism.kotlin.protos.DIDData
import io.iohk.atala.prism.kotlin.protos.TimestampInfo

@JsExport
fun findPublicKeyJS(didData: DIDData, keyId: String): KeyDataJS? =
    didData.findPublicKey(keyId)?.toJs()

@JsExport
fun toTimestampInfoModelJS(timestampInfo: TimestampInfo): TimestampInfoJS =
    timestampInfo.toTimestampInfoModel().toJs()
