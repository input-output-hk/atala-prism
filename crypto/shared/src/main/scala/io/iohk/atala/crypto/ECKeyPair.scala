package io.iohk.atala.crypto

trait ECKeyPair {
  def getPrivateKey: ECPrivateKey

  def getPublicKey: ECPublicKey
}

trait ECKey {
  def getEncoded: Array[Byte]

  def getHexEncoded: String
}

trait ECPrivateKey extends ECKey {}

trait ECPublicKey extends ECKey {}
