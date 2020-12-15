package io.iohk.atala.prism.kotlin.crypto

import io.iohk.atala.prism.kotlin.crypto.ECConfig.CURVE_NAME
import io.iohk.atala.prism.kotlin.crypto.keys.ECKeyPair
import io.iohk.atala.prism.kotlin.crypto.keys.ECPrivateKey
import io.iohk.atala.prism.kotlin.crypto.keys.ECPublicKey
import org.spongycastle.jce.provider.BouncyCastleProvider
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.Security
import java.security.spec.ECGenParameterSpec

actual object EC {
    private val provider = BouncyCastleProvider()
    init {
        Security.addProvider(provider)
    }

    actual fun generateKeyPair(): ECKeyPair {
        val keyGen = KeyPairGenerator.getInstance("ECDSA", provider)
        val ecSpec = ECGenParameterSpec(CURVE_NAME)
        keyGen.initialize(ecSpec, SecureRandom())
        val keyPair = keyGen.generateKeyPair()
        return ECKeyPair(ECPublicKey(keyPair.public), ECPrivateKey(keyPair.private))
    }
}
