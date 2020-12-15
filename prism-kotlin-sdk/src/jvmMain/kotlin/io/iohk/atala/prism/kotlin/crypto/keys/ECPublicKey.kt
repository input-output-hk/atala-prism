package io.iohk.atala.prism.kotlin.crypto.keys

import io.iohk.atala.prism.kotlin.crypto.ECConfig
import io.iohk.atala.prism.kotlin.crypto.util.toUnsignedByteArray
import java.lang.IllegalStateException
import java.security.PublicKey

actual class ECPublicKey(private val key: PublicKey) : ECKey() {
    override fun getEncoded(): List<Byte> {
        val curvePoint = when (key) {
            is org.spongycastle.jcajce.provider.asymmetric.ec.BCECPublicKey -> {
                val point = key.w
                ECPoint(point.affineX, point.affineY)
            }
            else -> throw IllegalStateException("Unexpected public key implementation")
        }
        val size = ECConfig.PRIVATE_KEY_BYTE_SIZE
        val xArr = curvePoint.x.toUnsignedByteArray()
        val yArr = curvePoint.y.toUnsignedByteArray()
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
}
