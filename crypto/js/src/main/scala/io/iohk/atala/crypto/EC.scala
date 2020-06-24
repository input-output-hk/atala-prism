package io.iohk.atala.crypto

import io.iohk.atala.crypto.facades.JsNativeEC

/**
  * JavaScript implementation of {@link ECTrait}.
  */
object EC extends ECTrait {
  override def generateKeyPair(): ECKeyPair = {
    val ec = new JsNativeEC(CURVE_NAME)
    JsECKeyPair(ec.genKeyPair())
  }
}
