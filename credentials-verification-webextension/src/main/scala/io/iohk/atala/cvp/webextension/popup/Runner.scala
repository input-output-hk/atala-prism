package io.iohk.atala.cvp.webextension.popup

import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import io.iohk.atala.cvp.webextension.common.I18NMessages
import io.iohk.atala.cvp.webextension.facades.elliptic.EC
import org.scalajs.dom._

import scala.concurrent.ExecutionContext

class Runner(messages: I18NMessages, backgroundAPI: BackgroundAPI) {

  val CURVE_NAME = "secp256k1"

  def run(): Unit = {
    log("This was run by the popup script")
    document.write(s"<p>${messages.appName}!!!</p>")
    testEc()
    backgroundAPI.sendBrowserNotification(messages.appName, "I'm on the Pop-up")
  }

  def testEc(): Unit = {
    val ec = new EC(CURVE_NAME)
    val keyPair = ec.genKeyPair()

    val sk = keyPair.getPrivate()
    println("Private key (hex): " + keyPair.getPrivate("hex"))
    println("Private key (hex): " + sk.toString("hex"))
    println("Private key (decimal): " + sk.toString(10))
    println("Private key (decimal, aligned): " + sk.toString(10, 100))

    val pk = keyPair.getPublic()
    println("Public key (hex encoded): " + keyPair.getPublic("hex"))
    println("Public key (hex encoded): " + pk.encode("hex"))
    val x = pk.getX()
    val y = pk.getY()
    println(s"Public key (hex): ${x.toString("hex")}, ${y.toString(16)}")
    println(s"Public key (dec): ${x.toString(10)}, ${y.toString(10)}")

    val message = "deadbeef"
    val signature = keyPair.sign(message)

    println(s"Signature (hex):" + signature.toDER("hex"))
    println(s"Signature (dec): ${signature.r.toString(10)}, ${signature.s.toString(10)}")

    val result = keyPair.verify(message, signature)

    println(s"Verification result: $result")
  }

  private def log(msg: String): Unit = {
    println(s"popup: $msg")
  }
}

object Runner {

  def apply()(implicit ec: ExecutionContext): Runner = {
    val messages = new I18NMessages
    val backgroundAPI = new BackgroundAPI()
    new Runner(messages, backgroundAPI)
  }
}
