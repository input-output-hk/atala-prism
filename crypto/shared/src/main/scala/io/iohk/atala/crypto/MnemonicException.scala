package io.iohk.atala.crypto

sealed class MnemonicException(message: String, cause: Throwable) extends Exception(message, cause)

case class MnemonicLengthException(message: String, cause: Throwable) extends MnemonicException(message, cause)

case class MnemonicWordException(message: String, cause: Throwable) extends MnemonicException(message, cause)

case class MnemonicChecksumException(message: String, cause: Throwable) extends MnemonicException(message, cause)
