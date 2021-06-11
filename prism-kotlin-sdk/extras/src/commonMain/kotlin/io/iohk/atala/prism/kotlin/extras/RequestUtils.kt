package io.iohk.atala.prism.kotlin.extras

import com.benasher44.uuid.bytes
import com.benasher44.uuid.uuid4
import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.crypto.keys.ECPrivateKey
import io.iohk.atala.prism.kotlin.identity.DID.Companion.masterKeyId
import io.iohk.atala.prism.kotlin.protos.PrismMetadata
import pbandk.encodeToByteArray
import kotlin.js.JsExport

@JsExport
object RequestUtils {
    fun generateRequestMetadata(
        did: String,
        didPrivateKey: ECPrivateKey,
        request: pbandk.Message,
        nonce: ByteArray? = null
    ): PrismMetadata =
        generateBytesMetadata(did, didPrivateKey, request.encodeToByteArray(), nonce)

    fun generateBytesMetadata(
        did: String,
        didPrivateKey: ECPrivateKey,
        bytes: ByteArray,
        nonce: ByteArray? = null
    ): PrismMetadata {
        val requestNonce = nonce ?: uuid4().bytes
        val didSignature = EC.sign(
            requestNonce + bytes,
            didPrivateKey
        )
        return PrismMetadata(
            did = did,
            didKeyId = masterKeyId, // NOTE: For now this is hardcoded as there are no other keys in the DIDs
            didSignature = didSignature.getEncoded(),
            requestNonce = requestNonce
        )
    }
}
