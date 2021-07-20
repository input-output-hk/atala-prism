package io.iohk.atala.prism.kotlin.crypto.keys

import io.iohk.atala.prism.kotlin.crypto.ECConfig

abstract class ECPublicKeyCommon : ECKey() {
    /**
     * Guarantees to return a list of 65 bytes in the following form:
     *
     * 0x04 ++ xBytes ++ yBytes
     *
     * Where `xBytes` and `yBytes` represent a 32-byte coordinates of a point
     * on the secp256k1 elliptic curve, which follow the formula below:
     *
     * y^2 == x^3 + 7
     *
     * @return a list of 65 bytes that represent uncompressed public key
     */
    override fun getEncoded(): ByteArray {
        val size = ECConfig.PRIVATE_KEY_BYTE_SIZE
        val basePoint = getCurvePoint()
        val xArr = basePoint.x.bytes()
        val yArr = basePoint.y.bytes()
        if (xArr.size == size && yArr.size == size) {
            val arr = ByteArray(1 + 2 * size) { 0 }
            arr[0] = 4 // Uncompressed point indicator for encoding
            xArr.copyInto(arr, size - xArr.size + 1)
            yArr.copyInto(arr, arr.size - yArr.size)
            return arr
        } else {
            throw IllegalStateException("Point coordinates do not match field size")
        }
    }

    /**
     * @return a point from the Secp256k1 elliptic curve representing this public key
     */
    abstract fun getCurvePoint(): ECPoint
}

expect class ECPublicKey : ECPublicKeyCommon
