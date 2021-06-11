package io.iohk.atala.prism.kotlin.crypto.derivation

import kotlin.js.JsExport

@JsExport
sealed class MnemonicException(message: String?, cause: Throwable?) :
    Exception(message, cause)

@JsExport
class MnemonicLengthException(message: String?, cause: Throwable? = null) :
    MnemonicException(message, cause)

@JsExport
class MnemonicWordException(message: String?, cause: Throwable? = null) :
    MnemonicException(message, cause)

@JsExport
class MnemonicChecksumException(message: String?, cause: Throwable? = null) :
    MnemonicException(message, cause)
