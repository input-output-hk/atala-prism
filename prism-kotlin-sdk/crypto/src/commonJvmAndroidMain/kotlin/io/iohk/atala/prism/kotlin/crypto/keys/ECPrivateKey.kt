package io.iohk.atala.prism.kotlin.crypto.keys

import com.ionspin.kotlin.bignum.integer.BigInteger
import io.iohk.atala.prism.kotlin.crypto.GenericJavaCryptography
import io.iohk.atala.prism.kotlin.crypto.util.toKotlinBigInteger
import io.iohk.atala.prism.kotlin.crypto.util.toUnsignedByteList
import java.security.PrivateKey

actual class ECPrivateKey(internal val key: PrivateKey) : ECKey() {
    override fun getEncoded(): List<Byte> =
        GenericJavaCryptography.privateKeyD(key).toUnsignedByteList()

    actual fun getD(): BigInteger =
        GenericJavaCryptography.privateKeyD(key).toKotlinBigInteger()
}
