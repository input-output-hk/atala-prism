package io.iohk.atala.prism.kotlin.crypto.derivation

import org.bitcoinj.wallet.DeterministicSeed
import java.security.SecureRandom

class JvmKeyDerivation: KeyDerivation {
    override fun randomMnemonicCode(): MnemonicCode {
        val entropyBytes = SecureRandom.getSeed(DeterministicSeed.DEFAULT_SEED_ENTROPY_BITS / 8)
        val mnemonicWords = JvmMnemonic.bitcoinjMnemonic.toMnemonic(entropyBytes)

        return MnemonicCode(mnemonicWords)
    }
}
