package io.iohk.atala.prism.kotlin.crypto.keys

import io.iohk.atala.prism.kotlin.util.BytesOps

abstract class ECKey {
    abstract fun getEncoded(): List<Byte>
    
    @ExperimentalUnsignedTypes
    fun getHexEncoded(): String {
        return BytesOps.bytesToHex(getEncoded().map { it.toUByte() })
    }

    override fun hashCode(): Int {
        return getEncoded().hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return when(other) {
            is ECKey -> getEncoded() == other.getEncoded()
            else -> false
        }
    }
}
