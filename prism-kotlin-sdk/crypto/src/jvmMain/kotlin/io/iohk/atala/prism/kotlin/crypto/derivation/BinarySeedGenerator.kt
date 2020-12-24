package io.iohk.atala.prism.kotlin.crypto.derivation

actual class BinarySeedGenerator actual constructor(private val keyDerivation: KeyDerivation) {
    actual fun randomBinarySeed(passphrase: String): List<Byte> {
        val seed = keyDerivation.randomMnemonicCode()
        JvmMnemonic.bitcoinjMnemonic.check(seed.words)

        return org.bitcoinj.crypto.MnemonicCode.toSeed(seed.words, passphrase).toList()
    }
}
