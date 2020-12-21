package io.iohk.atala.prism.kotlin.crypto.keys

import com.ionspin.kotlin.bignum.integer.BigInteger

data class ECPoint(val x: BigInteger, val y: BigInteger)
