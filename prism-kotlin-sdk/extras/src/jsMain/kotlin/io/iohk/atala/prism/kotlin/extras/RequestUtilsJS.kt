package io.iohk.atala.prism.kotlin.extras

import io.iohk.atala.prism.kotlin.crypto.exposed.ECPrivateKeyJS
import io.iohk.atala.prism.kotlin.crypto.exposed.toKotlin
import io.iohk.atala.prism.kotlin.protos.PrismMetadata

@JsExport
object RequestUtilsJS {
    fun generateRequestMetadata(
        did: String,
        didPrivateKey: ECPrivateKeyJS,
        request: pbandk.Message,
        nonce: ByteArray? = null
    ): PrismMetadata =
        RequestUtils.generateRequestMetadata(
            did,
            didPrivateKey.toKotlin(),
            request,
            nonce
        )

    fun generateBytesMetadata(
        did: String,
        didPrivateKey: ECPrivateKeyJS,
        bytes: ByteArray,
        nonce: ByteArray? = null
    ): PrismMetadata =
        RequestUtils.generateBytesMetadata(
            did,
            didPrivateKey.toKotlin(),
            bytes,
            nonce
        )
}
