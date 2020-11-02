package io.iohk.atala.prism.crypto.japi

import java.math.BigInteger

import io.iohk.atala.prism.crypto

class ECKeyPairFacade(val keyPair: crypto.ECKeyPair) extends ECKeyPair {
  override def getPublic(): ECPublicKey = new ECPublicKeyFacade(keyPair.publicKey)

  override def getPrivate(): ECPrivateKey = new ECPrivateKeyFacade(keyPair.privateKey)
}

class ECPublicKeyFacade(val publicKey: crypto.ECPublicKey) extends ECPublicKey {
  override def getCurvePoint: ECPoint = new ECPointFacade(publicKey.getCurvePoint)

  override def getEncoded: Array[Byte] = publicKey.getEncoded

  override def getHexEncoded: String = publicKey.getHexEncoded
}

object ECPublicKeyFacade {
  def unwrap(privateKey: ECPublicKey): crypto.ECPublicKey = privateKey.asInstanceOf[ECPublicKeyFacade].publicKey
}

class ECPointFacade(val ecPoint: crypto.ECPoint) extends ECPoint {
  override def getX: BigInteger = ecPoint.x.bigInteger

  override def getY: BigInteger = ecPoint.y.bigInteger
}

class ECPrivateKeyFacade(val privateKey: crypto.ECPrivateKey) extends ECPrivateKey {
  override def getEncoded: Array[Byte] = privateKey.getEncoded

  override def getHexEncoded: String = privateKey.getHexEncoded

  override def getD: BigInteger = privateKey.getD.bigInteger
}

object ECPrivateKeyFacade {
  def unwrap(privateKey: ECPrivateKey): crypto.ECPrivateKey = privateKey.asInstanceOf[ECPrivateKeyFacade].privateKey
}
