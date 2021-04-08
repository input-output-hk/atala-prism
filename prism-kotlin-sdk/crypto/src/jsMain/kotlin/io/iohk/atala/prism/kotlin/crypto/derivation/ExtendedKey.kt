package io.iohk.atala.prism.kotlin.crypto.derivation

import io.iohk.atala.prism.kotlin.crypto.keys.ECKeyPair
import io.iohk.atala.prism.kotlin.crypto.keys.ECPrivateKey
import io.iohk.atala.prism.kotlin.crypto.keys.ECPublicKey

actual class ExtendedKey {
    actual fun path(): DerivationPath {
        TODO("Not yet implemented")
    }

    actual fun publicKey(): ECPublicKey {
        TODO("Not yet implemented")
    }

    actual fun privateKey(): ECPrivateKey {
        TODO("Not yet implemented")
    }

    actual fun keyPair(): ECKeyPair {
        TODO("Not yet implemented")
    }

    actual fun derive(axis: DerivationAxis): ExtendedKey {
        TODO("Not yet implemented")
    }
}
