package io.iohk.atala.prism.kotlin.crypto.keys

import com.ionspin.kotlin.bignum.integer.BigInteger
import io.iohk.atala.prism.kotlin.crypto.ECConfig
import io.iohk.atala.prism.kotlin.crypto.externals.base

actual class ECPublicKey(val basePoint: base.BasePoint) : ECKey() {

    override fun getEncoded(): List<Byte> {
        val size = ECConfig.PRIVATE_KEY_BYTE_SIZE
        val xArr = BigInteger.parseString(basePoint.getX().toString()).toByteArray()
        val yArr = BigInteger.parseString(basePoint.getY().toString()).toByteArray()
        if (xArr.size <= size && yArr.size <= size) {
            val arr = ByteArray(1 + 2 * size) { 0 }
            arr[0] = 4 // Uncompressed point indicator for encoding
            xArr.copyInto(arr, size - xArr.size + 1)
            yArr.copyInto(arr, arr.size - yArr.size)
            return arr.toList()
        } else {
            throw RuntimeException("Point coordinates do not match field size")
        }
    }

    actual fun getCurvePoint(): ECPoint {
        val x = BigInteger.parseString(basePoint.getX().toString())
        val y = BigInteger.parseString(basePoint.getY().toString())
        return ECPoint(x, y)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class.js != other::class.js) return false
        if (!super.equals(other)) return false

        other as ECPublicKey

        if (getHexEncoded() != other.getHexEncoded()) return false

        return true
    }

    override fun hashCode(): Int =
        getHexEncoded().hashCode()
}
