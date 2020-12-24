package io.iohk.atala.prism.kotlin.crypto.keys

import com.ionspin.kotlin.bignum.integer.BigInteger

expect class ECPrivateKey : ECKey {
    fun getD(): BigInteger
}
