package io.iohk.atala.prism.kotlin.extras

import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.crypto.util.BytesOps.hexToBytes
import io.iohk.atala.prism.kotlin.protos.PrismMetadata

@JsExport
object RequestUtilsJS {
    fun generateRequestMetadata(did: String, didPrivateKey: String, request: pbandk.Message): PrismMetadata =
        RequestUtils.generateRequestMetadata(
            did,
            EC.toPrivateKey(hexToBytes(didPrivateKey).map { it.toByte() }),
            request
        )
}
