package io.iohk.atala.prism.kotlin.crypto.derivation

actual object KeyDerivation {
    actual fun randomMnemonicCode(): MnemonicCode {
        TODO("Not yet implemented")
    }

    actual fun isValidMnemonicWord(word: String): Boolean {
        TODO("Not yet implemented")
    }

    actual fun getValidMnemonicWords(): List<String> {
        TODO("Not yet implemented")
    }

    actual fun binarySeed(
        seed: MnemonicCode,
        passphrase: String
    ): List<Byte> {
        TODO("Not yet implemented")
    }

    actual fun derivationRoot(seed: List<Byte>): ExtendedKey {
        TODO("Not yet implemented")
    }

    actual fun deriveKey(
        seed: List<Byte>,
        path: DerivationPath
    ): ExtendedKey {
        TODO("Not yet implemented")
    }
}
