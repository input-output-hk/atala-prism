package io.iohk.atala.prism.kotlin.crypto.util

import kotlinx.cinterop.*

@ExperimentalUnsignedTypes
fun CArrayPointer<UByteVar>.toUByteArray(length: Int): UByteArray = UByteArray(length) {
    this[it]
}
