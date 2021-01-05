package io.iohk.atala.prism.kotlin.crypto.derivation

import io.iohk.atala.prism.kotlin.crypto.keys.ECKeyPair
import io.iohk.atala.prism.kotlin.crypto.keys.ECPrivateKey
import io.iohk.atala.prism.kotlin.crypto.keys.ECPublicKey

interface ExtendedKey {
    /** Derivation path used to obtain such key */
    fun path(): DerivationPath

    /** Public key for this extended key */
    fun publicKey(): ECPublicKey

    /** Private key for this extended key */
    fun privateKey(): ECPrivateKey

    /** KeyPair for this extended key */
    fun keyPair(): ECKeyPair

    /** Generates child extended key for given index */
    fun derive(axis: DerivationAxis): ExtendedKey
}
