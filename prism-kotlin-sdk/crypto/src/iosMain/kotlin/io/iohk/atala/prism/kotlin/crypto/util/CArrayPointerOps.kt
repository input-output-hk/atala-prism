package io.iohk.atala.prism.kotlin.crypto.util

import kotlinx.cinterop.*

@ExperimentalUnsignedTypes
fun CArrayPointer<UByteVar>.toUByteArray(length: Int): UByteArray = UByteArray(length) {
    this[it]
}

@ExperimentalUnsignedTypes
fun UByteArray.toCArrayPointer(memScope: MemScope): CArrayPointer<UByteVar> {
    val array = memScope.allocArray<UByteVar>(this.size)
    val arrayPtr = array.getPointer(memScope)
    for (i in this.indices) arrayPtr[i] = this[i]
    return array
}
