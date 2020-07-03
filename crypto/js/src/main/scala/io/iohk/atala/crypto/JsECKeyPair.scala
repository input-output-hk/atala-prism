package io.iohk.atala.crypto

import io.iohk.atala.crypto.facades.{JsNativeBigNumber, JsNativeCurvePoint, JsNativeKeyPair, JsNativeReducedBigNumber}

private[crypto] class JsECKeyPair(val privateKey: JsECPrivateKey, val publicKey: JsECPublicKey) extends ECKeyPair {
  override def getPrivateKey: ECPrivateKey = privateKey

  override def getPublicKey: ECPublicKey = publicKey
}

object JsECKeyPair {
  def apply(keyPair: JsNativeKeyPair): JsECKeyPair = {
    new JsECKeyPair(new JsECPrivateKey(keyPair.getPrivate()), new JsECPublicKey(keyPair.getPublic()))
  }
}

private[crypto] class JsECPrivateKey(val privateKey: JsNativeBigNumber) extends ECPrivateKey {
  override def getD: BigInt = {
    val hexEncoded = privateKey.toString("hex")
    ECUtils.toBigInt(hexEncoded)
  }
}

private[crypto] class JsECPublicKey(val publicKey: JsNativeCurvePoint) extends ECPublicKey {
  override def getCurvePoint: ECPoint = {
    ECPoint(toBigInt(publicKey.getX()), toBigInt(publicKey.getY()))
  }

  private def toBigInt(reducedBigNumber: JsNativeReducedBigNumber): BigInt = {
    ECUtils.toBigInt(reducedBigNumber.toString("hex"))
  }
}
