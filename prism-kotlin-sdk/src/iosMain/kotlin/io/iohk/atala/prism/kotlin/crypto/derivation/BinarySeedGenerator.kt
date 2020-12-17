package io.iohk.atala.prism.kotlin.crypto.derivation

actual class BinarySeedGenerator actual constructor(private val keyDerivation: KeyDerivation) {
    @ExperimentalUnsignedTypes
    actual fun randomBinarySeed(passphrase: String): List<Byte> {
        TODO("Implement")
    }
}
