package io.iohk.atala.prism.kotlin.extras

import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.crypto.ECConfig
import io.iohk.atala.prism.kotlin.crypto.util.BytesOps.hexToBytes
import io.iohk.atala.prism.kotlin.identity.DID.Companion.masterKeyId
import io.iohk.atala.prism.kotlin.protos.ECKeyData
import io.iohk.atala.prism.kotlin.protos.KeyUsage
import io.iohk.atala.prism.kotlin.protos.PublicKey
import pbandk.ByteArr
import kotlin.test.Test
import kotlin.test.assertEquals

class ECPublicKeyOpsTest {
    @Test
    fun ecPublicKeyCanBeConvertedToKeyData() {
        val publicKey = EC.generateKeyPair().publicKey
        val keyData = publicKey.toECKeyData()

        assertEquals(ECConfig.CURVE_NAME, keyData.curve)
        assertEquals((ECConfig.PUBLIC_KEY_BYTE_SIZE - 1) / 2, keyData.x.array.size)
        assertEquals((ECConfig.PUBLIC_KEY_BYTE_SIZE - 1) / 2, keyData.y.array.size)
    }

    @Test
    fun keyDataCanBeConvertedToPublicKey() {
        val id = masterKeyId
        val keyUsage = KeyUsage.MASTER_KEY
        val x = "c775e7b757ede630cd0aa1113bd102661ab38829ca52a6422ab782862f268646"
        val y = "15e2b0d3c33891ebb0f1ef609ec419420c20e320ce94c65fbc8c3312448eb225"
        val keyData = ECKeyData(
            curve = ECConfig.CURVE_NAME,
            x = ByteArr(hexToBytes(x)),
            y = ByteArr(hexToBytes(y))
        )

        val actualPublicKey = keyData.toPublicKey(id, keyUsage)
        val expectedPublicKey = PublicKey(
            id = id,
            usage = keyUsage,
            keyData = PublicKey.KeyData.EcKeyData(keyData)
        )
        assertEquals(expectedPublicKey, actualPublicKey)
    }
}
