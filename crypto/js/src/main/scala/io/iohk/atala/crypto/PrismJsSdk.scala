package io.iohk.atala.crypto

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}
import scala.scalajs.js

@JSExportTopLevel(name = "PrismSdk")
@JSExportAll
object PrismJsSdk {
  def hello(name: String): String = {
    s"Hello $name"
  }

  def generateKeyPair(): js.Dictionary[_] = {
    val pair = EC.generateKeyPair()
    js.Dictionary(
      "privateKey" -> pair.getPrivateKey.getD.toString,
      "publicKey" -> js.Dictionary(
        "x" -> pair.getPublicKey.getCurvePoint.x.toString,
        "y" -> pair.getPublicKey.getCurvePoint.y.toString
      )
    )
  }
}
