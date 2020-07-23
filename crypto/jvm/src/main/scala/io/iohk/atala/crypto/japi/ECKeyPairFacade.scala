package io.iohk.atala.crypto.japi

import java.math.BigInteger

import io.iohk.atala.crypto

class ECKeyPairFacade(val keyPair: crypto.ECKeyPair) extends ECKeyPair {
  override def getPublic(): ECPublicKey = new ECPublicKeyFacade(keyPair.publicKey)

  override def getPrivate(): ECPrivateKey = new ECPrivateKeyFacade(keyPair.privateKey)
}

class ECPublicKeyFacade(val publicKey: crypto.ECPublicKey) extends ECPublicKey {
  override def getEncoded: Array[Byte] = publicKey.getEncoded

  override def getHexEncoded: String = publicKey.getHexEncoded
}

class ECPrivateKeyFacade(val privateKey: crypto.ECPrivateKey) extends ECPrivateKey {
  override def getEncoded: Array[Byte] = privateKey.getEncoded

  override def getHexEncoded: String = privateKey.getHexEncoded

  override def getD: BigInteger = privateKey.getD.bigInteger
}
