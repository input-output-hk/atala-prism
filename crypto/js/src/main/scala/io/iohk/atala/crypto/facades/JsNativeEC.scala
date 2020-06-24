package io.iohk.atala.crypto.facades

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

@js.native
@JSImport("elliptic", "ec", globalFallback = "elliptic")
class JsNativeEC(curveName: String) extends js.Object {
  def genKeyPair(): JsNativeKeyPair = js.native
}

@js.native
trait JsNativeKeyPair extends js.Object {
  def getPublic(): JsNativeCurvePoint = js.native

  def getPrivate(): JsNativeBigNumber = js.native
}

@js.native
trait JsNativeCurvePoint extends js.Object {
  def encode(format: String): String = js.native
}
