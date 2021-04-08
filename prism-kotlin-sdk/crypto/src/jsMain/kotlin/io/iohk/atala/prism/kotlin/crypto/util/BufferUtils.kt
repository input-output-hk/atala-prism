package io.iohk.atala.prism.kotlin.crypto.util

import Buffer
import org.khronos.webgl.get

fun Buffer.toByteArray(): ByteArray {
    val byteArray = ByteArray(length)
    for (i in byteArray.indices) {
        byteArray[i] = this[i]
    }

    return byteArray
}
