package io.iohk.atala.prism.kotlin.crypto.derivation

import org.bitcoinj.crypto.HDKeyDerivation
import org.bitcoinj.wallet.DeterministicSeed
import java.io.ByteArrayInputStream
import java.security.SecureRandom

actual object KeyDerivation {
    private fun mnemonicCodeEnglishInputStream(): ByteArrayInputStream {
        val wordsText = MnemonicCodeEnglish.wordList.joinToString("\n", "", "\n")
        return wordsText.byteInputStream()
    }

    private val bitcoinjMnemonic =
        org.bitcoinj.crypto.MnemonicCode(mnemonicCodeEnglishInputStream(), null)

    actual fun randomMnemonicCode(): MnemonicCode {
        val entropyBytes = SecureRandom.getSeed(DeterministicSeed.DEFAULT_SEED_ENTROPY_BITS / 8)
        val mnemonicWords = JvmMnemonic.bitcoinjMnemonic.toMnemonic(entropyBytes)

        return MnemonicCode(mnemonicWords)
    }

    actual fun isValidMnemonicWord(word: String): Boolean =
        MnemonicCodeEnglish.wordList.contains(word)

    actual fun getValidMnemonicWords(): List<String> =
        MnemonicCodeEnglish.wordList

    actual fun binarySeed(seed: MnemonicCode, passphrase: String): ByteArray {
        val javaWords = seed.words

        try {
            bitcoinjMnemonic.check(javaWords)
        } catch (e: org.bitcoinj.crypto.MnemonicException.MnemonicChecksumException) {
            throw MnemonicChecksumException(e.message, e)
        } catch (e: org.bitcoinj.crypto.MnemonicException.MnemonicWordException) {
            throw MnemonicWordException(e.message, e)
        } catch (e: org.bitcoinj.crypto.MnemonicException.MnemonicLengthException) {
            throw MnemonicLengthException(e.message, e)
        } catch (e: Throwable) {
            throw RuntimeException("Unexpected exception returned by MnemonicCode.check", e)
        }

        return org.bitcoinj.crypto.MnemonicCode.toSeed(javaWords, passphrase)
    }

    actual fun derivationRoot(seed: ByteArray): ExtendedKey =
        ExtendedKey(HDKeyDerivation.createMasterPrivateKey(seed))

    actual fun deriveKey(seed: ByteArray, path: DerivationPath): ExtendedKey =
        path.axes.fold(derivationRoot(seed)) { key, axis -> key.derive(axis) }
}
