package io.iohk.atala.prism.kotlin.crypto.derivation

import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.crypto.keys.ECKeyPair
import io.iohk.atala.prism.kotlin.crypto.keys.ECPrivateKey
import io.iohk.atala.prism.kotlin.crypto.keys.ECPublicKey
import io.iohk.atala.prism.kotlin.crypto.util.toKotlinBigInteger
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDKeyDerivation

class JvmExtendedKey(val key: DeterministicKey) : ExtendedKey {
    /** Derivation path used to obtain such key */
    override fun path(): DerivationPath =
        DerivationPath(key.path.map { axis -> DerivationAxis(axis.i) })

    /** Public key for this extended key */
    override fun publicKey(): ECPublicKey {
        val ecPoint = key.pubKeyPoint
        return EC.toPublicKey(
            ecPoint.xCoord.toBigInteger().toKotlinBigInteger(),
            ecPoint.yCoord.toBigInteger().toKotlinBigInteger()
        )
    }

    /** Private key for this extended key */
    override fun privateKey(): ECPrivateKey =
        EC.toPrivateKey(key.privKey.toKotlinBigInteger())

    /** KeyPair for this extended key */
    override fun keyPair(): ECKeyPair =
        ECKeyPair(publicKey(), privateKey())

    /** Generates child extended key for given index */
    override fun derive(axis: DerivationAxis): JvmExtendedKey =
        JvmExtendedKey(HDKeyDerivation.deriveChildKey(key, ChildNumber(axis.i)))
}
