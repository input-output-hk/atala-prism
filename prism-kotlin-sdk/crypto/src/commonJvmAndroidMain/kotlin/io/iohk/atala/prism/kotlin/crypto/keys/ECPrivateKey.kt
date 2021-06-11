package io.iohk.atala.prism.kotlin.crypto.keys

import com.ionspin.kotlin.bignum.integer.BigInteger
import io.iohk.atala.prism.kotlin.crypto.ECConfig
import io.iohk.atala.prism.kotlin.crypto.GenericJavaCryptography
import io.iohk.atala.prism.kotlin.crypto.util.toKotlinBigInteger
import io.iohk.atala.prism.kotlin.crypto.util.toUnsignedByteArray
import java.security.PrivateKey

actual class ECPrivateKey(internal val key: PrivateKey) : ECKey() {
    override fun getEncoded(): ByteArray {
        val d = GenericJavaCryptography.privateKeyD(key)
        val byteList = d.toUnsignedByteArray()
        val padding = ByteArray(ECConfig.PRIVATE_KEY_BYTE_SIZE - byteList.size) { 0 }
        return padding + byteList
    }

    actual fun getD(): BigInteger =
        GenericJavaCryptography.privateKeyD(key).toKotlinBigInteger()
}
