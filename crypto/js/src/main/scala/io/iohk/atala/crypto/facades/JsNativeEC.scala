package io.iohk.atala.crypto.facades

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

@js.native
@JSImport("elliptic", "ec", globalFallback = "elliptic")
class JsNativeEC(curveName: String) extends js.Object {
  def genKeyPair(): JsNativeKeyPair = js.native

  def keyFromPrivate(priv: String, enc: Int): JsNativeKeyPair = js.native

  def keyFromPublic(pub: js.Any, enc: js.Any = js.undefined): JsNativeKeyPair = js.native

  def sign(
      msg: String,
      key: JsNativeBigNumber,
      encoding: js.Any = js.undefined,
      options: js.Any = js.undefined
  ): JsNativeSignature = js.native

  def verify(msg: String, signature: String, key: JsNativeCurvePoint, enc: js.Any = js.undefined): Boolean =
    js.native
}

@js.native
trait JsNativeKeyPair extends js.Object {
  def getPublic(): JsNativeCurvePoint = js.native

  def getPrivate(): JsNativeBigNumber = js.native
}

@js.native
trait JsNativeCurvePoint extends js.Object {
  def getX(): JsNativeReducedBigNumber = js.native

  def getY(): JsNativeReducedBigNumber = js.native
}

@js.native
@JSImport("elliptic", "Signature")
class JsNativeSignature(val options: js.Any, val enc: String) extends js.Object {
  def r: JsNativeReducedBigNumber = js.native

  def s: JsNativeReducedBigNumber = js.native

  def toDER(encoding: String): String = js.native
}
