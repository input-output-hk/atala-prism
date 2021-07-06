package io.iohk.atala.prism.crypto

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
      "privateKey" -> pair.privateKey.getD.toString,
      "publicKey" -> js.Dictionary(
        "x" -> pair.publicKey.getCurvePoint.x.toString,
        "y" -> pair.publicKey.getCurvePoint.y.toString
      )
    )
  }
}
