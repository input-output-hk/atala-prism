package io.iohk.atala.prism.kotlin.crypto

import com.ionspin.kotlin.bignum.integer.BigInteger

object ECConfig {
    val CURVE_NAME = "secp256k1"
    val PRIVATE_KEY_BYTE_SIZE = 32 // EC curve point coordinates are 32 bytes long
    val PUBLIC_KEY_BYTE_SIZE = 65
    val SIGNATURE_ALGORITHM = "SHA256withECDSA"
    val SIGNATURE_BYTE_SIZE = 2 * PRIVATE_KEY_BYTE_SIZE
    // Field characteristic p (prime) is equal to 2^256 - 2^32 - 2^9 - 2^8 - 2^7 - 2^6 - 2^4 - 1
    val p = BigInteger.parseString("115792089237316195423570985008687907853269984665640564039457584007908834671663", 10)
    val b = BigInteger(7)
}
