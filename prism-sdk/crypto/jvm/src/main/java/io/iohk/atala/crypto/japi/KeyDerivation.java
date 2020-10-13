package io.iohk.atala.crypto.japi;

import io.iohk.atala.crypto.AndroidKeyDerivation$;
import io.iohk.atala.crypto.KeyDerivation$;
import io.iohk.atala.crypto.MnemonicException;

import java.util.Arrays;
import java.util.List;

/**
 * These methods should be enough to implement our key derivation strategy:
 * - https://github.com/input-output-hk/atala/blob/develop/credentials-verification/docs/protocol/key-derivation.md
 *
 * The goal is to be able to use it on the Android app, and on the Browser Wallet.
 */
public interface KeyDerivation {
    public static KeyDerivation getInstance(CryptoProvider provider) {
        switch (provider) {
            case JVM:
                return new KeyDerivationFacade(KeyDerivation$.MODULE$);
            case Android:
                return new KeyDerivationFacade(AndroidKeyDerivation$.MODULE$);
            default:
                throw new IllegalArgumentException(
                        String.format("Unexpected provider %s, available types are %s",
                                provider, Arrays.toString(CryptoProvider.values())));
        }
    }

    /**
     * Generates a random mnemonic code, usually used when a new wallet is being created.
     */
    MnemonicCode randomMnemonicCode();

    /**
     * Checks if the word is one of words used in mnemonics
     */
    boolean isValidMnemonicWord(String word);

    /**
     * Returns list of valid mnemonic words
     */
    List<String> getValidMnemonicWords();

    /**
     * From the BIP39 spec (https://github.com/bitcoin/bips/blob/master/bip-0039.mediawiki#from-mnemonic-to-seed):
     * - To create a binary seed from the mnemonic, we use the PBKDF2 function with a mnemonic
     *   sentence (in UTF-8 NFKD) used as the password and the string "mnemonic" + passphrase (again in UTF-8 NFKD)
     *   used as the salt. The iteration count is set to 2048 and HMAC-SHA512 is used as the pseudo-random
     *   function. The length of the derived key is 512 bits (= 64 bytes).
     */
    byte[] binarySeed(MnemonicCode seed, String passphrase) throws MnemonicException;

    /**
     * Computes master key from seed bytes, according to BIP 32 protocol*/
    ExtendedKey derivationRoot(byte[] seed);

    /**
     * Computes key in derivation tree from seed bytes, according to BIP 32 protocol*/
    ExtendedKey deriveKey(byte[] seed, DerivationPath path);
}
