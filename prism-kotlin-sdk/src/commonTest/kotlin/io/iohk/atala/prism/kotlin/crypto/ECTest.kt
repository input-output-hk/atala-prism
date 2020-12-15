package io.iohk.atala.prism.kotlin.crypto

import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalUnsignedTypes
class ECTest {
    @Test
    fun testGeneration() {
        val keyPair = EC.generateKeyPair()
        assertEquals(ECConfig.PRIVATE_KEY_BYTE_SIZE * 2, keyPair.privateKey.getHexEncoded().length)
        assertEquals(ECConfig.PUBLIC_KEY_BYTE_SIZE * 2, keyPair.publicKey.getHexEncoded().length)
    }
}
