package io.iohk.atala.prism.crypto

import typings.bnJs.bnJsStrings
import typings.bnJs.mod.^
import typings.elliptic.mod.curve.base.BasePoint

private[crypto] class JsECPrivateKey(val privateKey: ^) extends ECPrivateKey {
  override def getD: BigInt = {
    val hexEncoded = privateKey.toString_hex(bnJsStrings.hex)
    ECUtils.toBigInt(hexEncoded)
  }
}

private[crypto] class JsECPublicKey(val publicKey: BasePoint) extends ECPublicKey {
  override def getCurvePoint: ECPoint = {
    ECPoint(toBigInt(publicKey.getX()), toBigInt(publicKey.getY()))
  }

  private def toBigInt(reducedBigNumber: ^): BigInt = {
    ECUtils.toBigInt(reducedBigNumber.toString_hex(bnJsStrings.hex))
  }
}
