package io.iohk.atala.prism.kotlin.crypto.derivation

sealed class MnemonicException(message: String?, cause: Throwable?) :
    Exception(message, cause)

class MnemonicLengthException(message: String?, cause: Throwable? = null) :
    MnemonicException(message, cause)

class MnemonicWordException(message: String?, cause: Throwable? = null) :
    MnemonicException(message, cause)

class MnemonicChecksumException(message: String?, cause: Throwable? = null) :
    MnemonicException(message, cause)
