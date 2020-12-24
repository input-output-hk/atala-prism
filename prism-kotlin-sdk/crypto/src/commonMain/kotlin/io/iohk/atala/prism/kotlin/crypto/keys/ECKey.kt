package io.iohk.atala.prism.kotlin.crypto.keys

import io.iohk.atala.prism.kotlin.util.BytesOps

abstract class ECKey {
    /**
     * Guarantees to return a list of 65 bytes in the following form:
     * 
     * 0x04 ++ xBytes ++ yBytes
     * 
     * Where `xBytes` and `yBytes` represent a 32-byte coordinates of a point
     * on the secp256k1 elliptic curve, which follow the formula below:
     * 
     * y^2 == x^3 + 7
     */
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
