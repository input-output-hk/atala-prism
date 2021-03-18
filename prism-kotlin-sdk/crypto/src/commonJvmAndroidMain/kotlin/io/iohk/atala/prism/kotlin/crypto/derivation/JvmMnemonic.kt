package io.iohk.atala.prism.kotlin.crypto.derivation

import java.io.ByteArrayInputStream

object JvmMnemonic {
    private val wordsText = MnemonicCodeEnglish.wordList.joinToString(separator = "\n", postfix = "\n")
    private val mnemonicCodeEnglishInputStream = ByteArrayInputStream(wordsText.toByteArray())
    val bitcoinjMnemonic = org.bitcoinj.crypto.MnemonicCode(mnemonicCodeEnglishInputStream, null)
}
