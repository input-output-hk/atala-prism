package io.iohk.atala.crypto.facades

import scala.scalajs.js

@js.native
trait JsNativeBigNumber extends js.Object {
  def toString(encoding: String): String = js.native
}

@js.native
trait JsNativeReducedBigNumber extends js.Object {
  def toString(encoding: String): String = js.native
}
