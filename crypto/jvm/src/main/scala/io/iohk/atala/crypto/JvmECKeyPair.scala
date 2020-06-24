package io.iohk.atala.crypto

import java.security.{Key => JavaKey, KeyPair => JavaKeyPair, PrivateKey => JavaPrivateKey, PublicKey => JavaPublicKey}
import javax.xml.bind.DatatypeConverter

private[crypto] class JvmECKeyPair(val privateKey: JvmECPrivateKey, val publicKey: JvmECPublicKey) extends ECKeyPair {
  override def getPrivateKey: ECPrivateKey = privateKey

  override def getPublicKey: ECPublicKey = publicKey
}

object JvmECKeyPair {
  def apply(keyPair: JavaKeyPair): JvmECKeyPair = {
    new JvmECKeyPair(new JvmECPrivateKey(keyPair.getPrivate), new JvmECPublicKey(keyPair.getPublic))
  }
}

private class JvmECKey(private val key: JavaKey) extends ECKey {
  override def getEncoded: Array[Byte] = key.getEncoded

  override def getHexEncoded: String = DatatypeConverter.printHexBinary(getEncoded)
}

private class JvmECPrivateKey(private val key: JavaPrivateKey) extends JvmECKey(key) with ECPrivateKey {}

private class JvmECPublicKey(private val key: JavaPublicKey) extends JvmECKey(key) with ECPublicKey {}
