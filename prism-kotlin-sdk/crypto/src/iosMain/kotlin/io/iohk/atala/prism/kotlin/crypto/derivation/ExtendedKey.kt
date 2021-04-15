package io.iohk.atala.prism.kotlin.crypto.derivation

import fr.acinq.bitcoin.DeterministicWallet
import fr.acinq.bitcoin.KeyPath
import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.crypto.keys.ECKeyPair
import io.iohk.atala.prism.kotlin.crypto.keys.ECPrivateKey
import io.iohk.atala.prism.kotlin.crypto.keys.ECPublicKey

private fun toDerivationAxis(keyPath: KeyPath): DerivationPath =
    DerivationPath(keyPath.path.map { DerivationAxis(it.toInt()) })

actual class ExtendedKey(private val key: DeterministicWallet.ExtendedPrivateKey) {
    actual fun path(): DerivationPath =
        toDerivationAxis(key.path)

    actual fun publicKey(): ECPublicKey =
        EC.toPublicKey(key.publicKey.toUncompressedBin().toList())

    actual fun privateKey(): ECPrivateKey =
        EC.toPrivateKey(key.privateKey.value.toByteArray().toList())

    actual fun keyPair(): ECKeyPair =
        ECKeyPair(publicKey(), privateKey())

    actual fun derive(axis: DerivationAxis): ExtendedKey {
        val index =
            if (axis.hardened)
                DeterministicWallet.hardened(axis.number.toLong())
            else
                axis.number.toLong()
        return ExtendedKey(DeterministicWallet.derivePrivateKey(key, index))
    }
}
