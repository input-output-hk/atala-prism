package io.iohk.atala.prism.kotlin.extras

import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.identity.DID.Companion.masterKeyId
import io.iohk.atala.prism.kotlin.protos.*
import kotlinx.datetime.Clock
import pbandk.wkt.Timestamp
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ProtosOpsTest {
    private val instant = Clock.System.now()
    private val masterPublicKeyData = EC.generateKeyPair().publicKey.toECKeyData()
    private val master = PublicKey(
        id = masterKeyId,
        usage = KeyUsage.MASTER_KEY,
        addedOn = TimestampInfo(
            blockTimestamp = Timestamp(instant.epochSeconds, instant.nanosecondsOfSecond)
        ),
        keyData = PublicKey.KeyData.EcKeyData(masterPublicKeyData)
    )
    private val issuingPublicKeyData = EC.generateKeyPair().publicKey.toECKeyData()
    private val issuing = PublicKey(
        id = "issuing0",
        usage = KeyUsage.ISSUING_KEY,
        addedOn = TimestampInfo(
            blockTimestamp = Timestamp(instant.epochSeconds, instant.nanosecondsOfSecond)
        ),
        keyData = PublicKey.KeyData.EcKeyData(issuingPublicKeyData)
    )
    private val didData = DIDData(publicKeys = listOf(master, issuing))

    @Test
    fun canFindPublicKeyInDidData() {
        assertNotNull(didData.findPublicKey(masterKeyId))
        assertNotNull(didData.findPublicKey("issuing0"))
    }

    @Test
    fun doesNotFailOnNonExistentKeyId() {
        assertNull(didData.findPublicKey("authentication0"))
    }
}
