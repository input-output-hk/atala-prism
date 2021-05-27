package io.iohk.atala.prism.kotlin.extras

import com.benasher44.uuid.bytes
import com.benasher44.uuid.uuid4
import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.crypto.keys.ECPrivateKey
import io.iohk.atala.prism.kotlin.identity.DID.Companion.masterKeyId
import io.iohk.atala.prism.kotlin.protos.PrismMetadata
import pbandk.encodeToByteArray

object RequestUtils {

    fun generateRequestMetadata(did: String, didPrivateKey: ECPrivateKey, request: pbandk.Message): PrismMetadata {
        val requestNonce = uuid4().bytes.toList()
        val didSignature = EC.sign(
            requestNonce + request.encodeToByteArray().toList(),
            didPrivateKey
        )
        return PrismMetadata(
            did = did,
            didKeyId = masterKeyId, // NOTE: For now this is hardcoded as there are no other keys in the DIDs
            didSignature = didSignature.getEncoded().toByteArray(),
            requestNonce = requestNonce.toByteArray()
        )
    }
}
