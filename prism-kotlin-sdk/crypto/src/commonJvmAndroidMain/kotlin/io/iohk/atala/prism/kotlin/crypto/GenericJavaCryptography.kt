package io.iohk.atala.prism.kotlin.crypto

import java.math.BigInteger
import java.security.PrivateKey
import java.security.Provider
import java.security.PublicKey
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.KeySpec

expect object GenericJavaCryptography {
    val provider: Provider
    val ecNamedCurveSpec: ECParameterSpec

    fun keySpec(d: BigInteger): KeySpec
    fun privateKeyD(privateKey: PrivateKey): BigInteger
    fun publicKeyPoint(publicKey: PublicKey): ECPoint
}
