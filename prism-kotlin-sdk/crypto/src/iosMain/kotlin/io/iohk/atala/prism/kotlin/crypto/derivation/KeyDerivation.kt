package io.iohk.atala.prism.kotlin.crypto.derivation

import fr.acinq.bitcoin.DeterministicWallet
import io.iohk.atala.prism.kotlin.crypto.util.toUByteArray
import kotlinx.cinterop.*
import platform.posix.*

actual object KeyDerivation {
    private fun generateBytes(memScope: MemScope, numBytes: Int): CArrayPointer<UByteVar> {
        val result = memScope.allocArray<UByteVar>(numBytes)
        val resultPtr = result.getPointer(memScope)
        val urandom = fopen("/dev/urandom", "rb") ?: error("No /dev/urandom on this device")
        try {
            fread(resultPtr, 1.convert(), numBytes.convert(), urandom)
            for (n in 0 until numBytes) result[n] = resultPtr[n]
        } finally {
            fclose(urandom)
        }
        return result
    }

    actual fun randomMnemonicCode(): MnemonicCode {
        val mnemonicWords = memScoped {
            val entropyPtr = generateBytes(this, 16)
            val entropyBytes = entropyPtr.toUByteArray(16).toByteArray()
            fr.acinq.bitcoin.MnemonicCode.toMnemonics(entropyBytes)
        }

        return MnemonicCode(mnemonicWords)
    }

    actual fun isValidMnemonicWord(word: String): Boolean =
        MnemonicCodeEnglish.wordList.contains(word)

    actual fun getValidMnemonicWords(): List<String> =
        MnemonicCodeEnglish.wordList

    actual fun binarySeed(seed: MnemonicCode, passphrase: String): ByteArray {
        try {
            fr.acinq.bitcoin.MnemonicCode.validate(seed.words, MnemonicCodeEnglish.wordList)
        } catch (e: RuntimeException) {
            when {
                e.message == "invalid checksum" -> {
                    throw MnemonicChecksumException(e.message, e)
                }
                e.message == "mnemonic code cannot be empty" ||
                    e.message?.contains("invalid mnemonic word count") == true -> {
                    throw MnemonicLengthException(e.message, e)
                }
                e.message?.contains("invalid mnemonic word") == true -> {
                    throw MnemonicWordException(e.message, e)
                }
                else -> {
                    throw e
                }
            }
        }

        return fr.acinq.bitcoin.MnemonicCode.toSeed(seed.words, passphrase)
    }

    actual fun derivationRoot(seed: ByteArray): ExtendedKey =
        ExtendedKey(DeterministicWallet.generate(seed))

    actual fun deriveKey(seed: ByteArray, path: DerivationPath): ExtendedKey =
        path.axes.fold(derivationRoot(seed)) { key, axis -> key.derive(axis) }
}
