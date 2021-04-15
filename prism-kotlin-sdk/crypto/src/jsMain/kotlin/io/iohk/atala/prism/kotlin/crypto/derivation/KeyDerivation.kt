package io.iohk.atala.prism.kotlin.crypto.derivation

import io.iohk.atala.prism.kotlin.crypto.externals.fromSeed
import io.iohk.atala.prism.kotlin.crypto.externals.generateMnemonic
import io.iohk.atala.prism.kotlin.crypto.externals.mnemonicToSeedSync
import io.iohk.atala.prism.kotlin.crypto.externals.validateMnemonic
import io.iohk.atala.prism.kotlin.crypto.util.toByteArray

actual object KeyDerivation {
    private val wordArray = MnemonicCodeEnglish.wordList.toTypedArray()

    actual fun randomMnemonicCode(): MnemonicCode {
        val words = generateMnemonic(wordlist = wordArray)
        return MnemonicCode(words.split(' '))
    }

    actual fun isValidMnemonicWord(word: String): Boolean =
        MnemonicCodeEnglish.wordList.contains(word)

    actual fun getValidMnemonicWords(): List<String> =
        MnemonicCodeEnglish.wordList

    actual fun binarySeed(
        seed: MnemonicCode,
        passphrase: String
    ): List<Byte> {
        val mnemonic = seed.words.joinToString(" ")

        if (seed.words.size % 3 != 0) {
            throw MnemonicLengthException("Word list size must be multiple of three words")
        } else if (seed.words.isEmpty()) {
            throw MnemonicLengthException("Word list is empty")
        }
        for (word in seed.words) {
            if (!isValidMnemonicWord(word)) {
                throw MnemonicWordException("Invalid mnemonic word: $word")
            }
        }

        if (!validateMnemonic(mnemonic, wordArray)) {
            throw MnemonicChecksumException("Invalid mnemonic checksum")
        }

        return mnemonicToSeedSync(mnemonic, passphrase).toByteArray().toList()
    }

    actual fun derivationRoot(seed: List<Byte>): ExtendedKey {
        val bip32 = fromSeed(Buffer.from(seed.toTypedArray()))
        return ExtendedKey(bip32, DerivationPath.empty())
    }

    actual fun deriveKey(
        seed: List<Byte>,
        path: DerivationPath
    ): ExtendedKey =
        path.axes.fold(derivationRoot(seed)) { key, axis -> key.derive(axis) }
}
