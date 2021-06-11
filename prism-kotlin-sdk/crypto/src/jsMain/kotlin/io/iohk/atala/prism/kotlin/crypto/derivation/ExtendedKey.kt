package io.iohk.atala.prism.kotlin.crypto.derivation

import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.crypto.externals.BIP32Interface
import io.iohk.atala.prism.kotlin.crypto.keys.ECKeyPair
import io.iohk.atala.prism.kotlin.crypto.keys.ECPrivateKey
import io.iohk.atala.prism.kotlin.crypto.keys.ECPublicKey
import io.iohk.atala.prism.kotlin.crypto.util.toByteArray

actual class ExtendedKey(
    private val bip32: BIP32Interface,
    private val path: DerivationPath
) {
    actual fun path(): DerivationPath =
        path

    actual fun publicKey(): ECPublicKey =
        EC.toPublicKeyFromPrivateKey(privateKey())

    actual fun privateKey(): ECPrivateKey =
        EC.toPrivateKey(bip32.privateKey!!.toByteArray())

    actual fun keyPair(): ECKeyPair =
        ECKeyPair(publicKey(), privateKey())

    actual fun derive(axis: DerivationAxis): ExtendedKey {
        val derivedBip32 =
            if (axis.hardened)
                bip32.deriveHardened(axis.number)
            else
                bip32.derive(axis.number)
        return ExtendedKey(derivedBip32, path.derive(axis))
    }
}
