package io.iohk.atala.crypto

import java.math.BigInteger

import io.iohk.atala.crypto.facades.{JsNativeBigNumber, JsNativeCurvePoint, JsNativeKeyPair}

private[crypto] class JsECKeyPair(val privateKey: JsECPrivateKey, val publicKey: JsECPublicKey) extends ECKeyPair {
  override def getPrivateKey: ECPrivateKey = privateKey

  override def getPublicKey: ECPublicKey = publicKey
}

object JsECKeyPair {
  def apply(keyPair: JsNativeKeyPair): JsECKeyPair = {
    new JsECKeyPair(new JsECPrivateKey(keyPair.getPrivate()), new JsECPublicKey(keyPair.getPublic()))
  }
}

private abstract class JsECKey extends ECKey {
  override def getEncoded: Array[Byte] = new BigInteger(getHexEncoded, 16).toByteArray
}

private class JsECPrivateKey(val privateKey: JsNativeBigNumber) extends JsECKey with ECPrivateKey {
  override def getHexEncoded: String = privateKey.toString("hex")
}

private class JsECPublicKey(val publicKey: JsNativeCurvePoint) extends JsECKey with ECPublicKey {
  override def getHexEncoded: String = publicKey.encode("hex")
}
