package io.iohk.atala.prism.kotlin.crypto.derivation

import cocoapods.BitcoinKit._Key
import io.iohk.atala.prism.kotlin.crypto.util.toUtf8NsData
import kotlinx.cinterop.readBytes
import platform.Foundation.*

actual class BinarySeedGenerator actual constructor(private val keyDerivation: KeyDerivation) {
    @ExperimentalUnsignedTypes
    actual fun randomBinarySeed(passphrase: String): List<Byte> {
        val mnemonic = keyDerivation.randomMnemonicCode()
        val mnemonicData = mnemonic.words.joinToString(separator = " ").toUtf8NsData()!!
        val saltData = "mnemonic$passphrase".toUtf8NsData()!!

        val seedNSData = _Key.deriveKey(mnemonicData, saltData, 2048, 64)
        return seedNSData.bytes?.readBytes(seedNSData.length.toInt())!!.toList()
    }
}
