package io.iohk.atala.crypto

import java.security.{KeyPair => JavaKeyPair, PrivateKey => JavaPrivateKey, PublicKey => JavaPublicKey}

import org.bouncycastle.jcajce.provider.asymmetric.ec.{BCECPrivateKey, BCECPublicKey}

private[crypto] class JvmECKeyPair(val privateKey: JvmECPrivateKey, val publicKey: JvmECPublicKey) extends ECKeyPair {
  override def getPrivateKey: ECPrivateKey = privateKey

  override def getPublicKey: ECPublicKey = publicKey
}

private[crypto] object JvmECKeyPair {
  def apply(keyPair: JavaKeyPair): JvmECKeyPair = {
    new JvmECKeyPair(new JvmECPrivateKey(keyPair.getPrivate), new JvmECPublicKey(keyPair.getPublic))
  }
}

private[crypto] class JvmECPrivateKey(val key: JavaPrivateKey) extends ECPrivateKey {
  override def getD: BigInt = {
    key match {
      case k: BCECPrivateKey => k.getD
    }
  }
}

private[crypto] class JvmECPublicKey(val key: JavaPublicKey) extends ECPublicKey {
  override def getCurvePoint: ECPoint = {
    key match {
      case k: BCECPublicKey =>
        val point = k.getW
        ECPoint(point.getAffineX, point.getAffineY)
    }
  }
}
