package io.iohk.atala.prism.kotlin.crypto.derivation

expect class BinarySeedGenerator(keyDerivation: KeyDerivation) {
    fun randomBinarySeed(passphrase: String): List<Byte>
}
