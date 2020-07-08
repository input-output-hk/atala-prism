package io.iohk.atala.crypto

import java.security.{PrivateKey => JavaPrivateKey, PublicKey => JavaPublicKey}

private[crypto] class JvmECPrivateKey(val key: JavaPrivateKey) extends ECPrivateKey {
  override def getD: BigInt = {

    key match {
      case k: org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey => k.getD
      case k: org.spongycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey => k.getD
    }
  }
}

private[crypto] class JvmECPublicKey(val key: JavaPublicKey) extends ECPublicKey {
  override def getCurvePoint: ECPoint = {
    key match {
      case k: org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey =>
        val point = k.getW
        ECPoint(point.getAffineX, point.getAffineY)

      case k: org.spongycastle.jcajce.provider.asymmetric.ec.BCECPublicKey =>
        val point = k.getW
        ECPoint(point.getAffineX, point.getAffineY)
    }
  }
}
