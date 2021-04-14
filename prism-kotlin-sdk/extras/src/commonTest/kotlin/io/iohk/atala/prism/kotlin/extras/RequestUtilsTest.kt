package io.iohk.atala.prism.kotlin.extras

import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.identity.DID
import io.iohk.atala.prism.kotlin.protos.HealthCheckRequest
import pbandk.encodeToByteArray
import kotlin.test.Test
import kotlin.test.assertTrue

class RequestUtilsTest {
    @Test
    fun generatesVerifiableMetadata() {
        val keyPair = EC.generateKeyPair()
        val did = DID.createUnpublishedDID(keyPair.publicKey)
        val message = HealthCheckRequest()
        val metadata = RequestUtils.generateRequestMetadata(did.value, keyPair.privateKey, message)
        assertTrue(
            EC.verify(
                (metadata.requestNonce + message.encodeToByteArray()).toList(),
                keyPair.publicKey,
                EC.toSignature(metadata.didSignature.toList())
            )
        )
    }
}
