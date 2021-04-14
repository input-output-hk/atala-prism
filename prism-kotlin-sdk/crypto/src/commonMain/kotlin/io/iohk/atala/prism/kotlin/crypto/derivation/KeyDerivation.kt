package io.iohk.atala.prism.kotlin.crypto.derivation

/**
 * These methods should be enough to implement our key derivation strategy:
 * - https://github.com/input-output-hk/atala/blob/develop/credentials-verification/docs/protocol/key-derivation.md
 *
 * The goal is to be able to use it on the Android app, and on the Browser Wallet.
 */
expect object KeyDerivation {
    /**
     * Generates a random mnemonic code, usually used when a new wallet is being created.
     */
    fun randomMnemonicCode(): MnemonicCode

    /** Checks if the word is one of words used in mnemonics */
    fun isValidMnemonicWord(word: String): Boolean

    /** Returns list of valid mnemonic words */
    fun getValidMnemonicWords(): List<String>

    /**
     * From the BIP39 spec (https://github.com/bitcoin/bips/blob/master/bip-0039.mediawiki#from-mnemonic-to-seed):
     * - To create a binary seed from the mnemonic, we use the PBKDF2 function with a mnemonic
     *   sentence (in UTF-8 NFKD) used as the password and the string "mnemonic" + passphrase (again in UTF-8 NFKD)
     *   used as the salt. The iteration count is set to 2048 and HMAC-SHA512 is used as the pseudo-random
     *   function. The length of the derived key is 512 bits (= 64 bytes).
     */
    fun binarySeed(seed: MnemonicCode, passphrase: String): List<Byte>

    /** Computes master key from seed bytes, according to BIP 32 protocol*/
    fun derivationRoot(seed: List<Byte>): ExtendedKey

    /** Computes key in derivation tree from seed bytes, according to BIP 32 protocol*/
    fun deriveKey(seed: List<Byte>, path: DerivationPath): ExtendedKey
}
