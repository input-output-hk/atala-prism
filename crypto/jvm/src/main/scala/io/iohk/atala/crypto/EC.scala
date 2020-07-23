package io.iohk.atala.crypto

import java.security.spec.{KeySpec, ECParameterSpec => JavaECParameterSpec}

import io.iohk.atala.crypto.ECConfig.CURVE_NAME
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.{ECNamedCurveSpec, ECPublicKeySpec => BCECPublicKeySpec}

/**
  * JVM implementation of {@link ECTrait}.
  */
object EC extends GenericEC(new BouncyCastleProvider) {
  private lazy val ecParameterSpec = ECNamedCurveTable.getParameterSpec(CURVE_NAME)

  def instance: GenericEC = this

  override protected val ecNamedCurveSpec: JavaECParameterSpec = new ECNamedCurveSpec(
    ecParameterSpec.getName,
    ecParameterSpec.getCurve,
    ecParameterSpec.getG,
    ecParameterSpec.getN
  )

  override protected def getCurveFieldSize: Int = {
    ecParameterSpec.getCurve.getFieldSize
  }

  override protected def keySpec(d: BigInt): KeySpec = {
    val Q = ecParameterSpec.getG.multiply(d.bigInteger)
    new BCECPublicKeySpec(Q, ecParameterSpec)
  }
}
