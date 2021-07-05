package io.iohk.atala.prism.kotlin.extras

import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.crypto.derivation.KeyDerivation
import io.iohk.atala.prism.kotlin.crypto.derivation.KeyType
import io.iohk.atala.prism.kotlin.identity.DID
import io.iohk.atala.prism.kotlin.protos.HealthCheckRequest
import pbandk.encodeToByteArray
import kotlin.test.Test
import kotlin.test.assertTrue

class RequestUtilsTest {
    @Test
    fun generatesVerifiableMetadata() {
        val didIndex = 107
        val mnemonic = KeyDerivation.randomMnemonicCode()
        val seed = KeyDerivation.binarySeed(mnemonic, "password")
        val keyPair = DID.deriveKeyFromFullPath(seed, didIndex, KeyType.MASTER_KEY, 0)

        val did = DID.createDIDFromMnemonic(mnemonic, didIndex).unpublishedDID
        val message = HealthCheckRequest()
        val metadata = RequestUtils.generateRequestMetadata(did.value, keyPair.privateKey, message)
        assertTrue(
            EC.verify(
                (metadata.requestNonce + message.encodeToByteArray()),
                keyPair.publicKey,
                EC.toSignature(metadata.didSignature)
            )
        )
    }
}
