package io.iohk.atala.prism.kotlin.crypto.derivation

interface KeyDerivation {
    fun randomMnemonicCode(): MnemonicCode
}
