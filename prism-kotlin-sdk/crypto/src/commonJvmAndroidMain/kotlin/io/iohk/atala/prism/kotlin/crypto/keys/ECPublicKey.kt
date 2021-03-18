package io.iohk.atala.prism.kotlin.crypto.keys

import com.ionspin.kotlin.bignum.integer.BigInteger
import com.ionspin.kotlin.bignum.integer.Sign
import io.iohk.atala.prism.kotlin.crypto.ECConfig
import io.iohk.atala.prism.kotlin.crypto.GenericJavaCryptography
import io.iohk.atala.prism.kotlin.crypto.util.toKotlinBigInteger
import io.iohk.atala.prism.kotlin.crypto.util.toUnsignedByteArray
import java.security.PublicKey

actual class ECPublicKey(internal val key: PublicKey) : ECKey() {
    override fun getEncoded(): List<Byte> {
        val javaPoint = GenericJavaCryptography.publicKeyPoint(key)
        val curvePoint = ECPoint(
            BigInteger.fromByteArray(javaPoint.affineX.toUnsignedByteArray(), Sign.POSITIVE),
            BigInteger.fromByteArray(javaPoint.affineY.toUnsignedByteArray(), Sign.POSITIVE)
        )
        val size = ECConfig.PRIVATE_KEY_BYTE_SIZE
        val xArr = curvePoint.x.toByteArray()
        val yArr = curvePoint.y.toByteArray()
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
        val javaPoint = GenericJavaCryptography.publicKeyPoint(key)
        return ECPoint(javaPoint.affineX.toKotlinBigInteger(), javaPoint.affineY.toKotlinBigInteger())
    }
}
