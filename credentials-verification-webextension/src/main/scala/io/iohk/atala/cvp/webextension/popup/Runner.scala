package io.iohk.atala.cvp.webextension.popup

import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import io.iohk.atala.cvp.webextension.background.wallet.{Role, WalletManager}
import io.iohk.atala.cvp.webextension.common.I18NMessages
import io.iohk.atala.cvp.webextension.facades.elliptic.{EC, KeyPair}
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.{HTMLHeadElement, HTMLParagraphElement, HTMLSelectElement}
import typings.bip39.{mod => bip39}
import typings.bip32.{mod => bip32}
import typings.node.BufferEncoding
import typings.node.bufferMod.Buffer
import typings.std.document
import typings.std.console

import scala.concurrent.ExecutionContext
import scala.scalajs.js
import scala.util.{Failure, Success}

class Runner(messages: I18NMessages, backgroundAPI: BackgroundAPI)(implicit ec: ExecutionContext) {

  val CURVE_NAME = "secp256k1"

  def getRequestsListElement(): dom.raw.Element = dom.document.getElementById("requests-list")
  def getKeysSelectionElement(): dom.raw.HTMLSelectElement = {
    dom.document.getElementById("key-selection").asInstanceOf[HTMLSelectElement]
  }
  def getRequestsInfoElement(): dom.raw.HTMLParagraphElement = {
    dom.document.getElementById("requests-info").asInstanceOf[HTMLParagraphElement]
  }

  def loadRequests(): Unit = {
    backgroundAPI
      .getSignatureRequests()
      .onComplete {
        case Success(requests) =>
          val requestsList = getRequestsListElement()
          while (requestsList.hasChildNodes()) {
            requestsList.removeChild(requestsList.firstChild)
          }

          for (request <- requests.requests) {
            val requestId = request.id
            val el = dom.document.createElement("li")
            el.textContent = request.message
            val onclick: js.Function1[Event, Unit] = { ev =>
              signRequest(requestId)
            }
            el.addEventListener("click", onclick, true)
            requestsList.appendChild(el)
          }

          val requestsInfo = getRequestsInfoElement()
          if (requests.requests.nonEmpty) {
            requestsInfo.textContent = "Click message to sign"
          } else {
            requestsInfo.textContent = "No requests"
          }
        case Failure(ex) =>
          log(s"Failed obtaining signature requests: ${ex.getMessage}")
          throw ex
      }
  }

  def signRequest(requestId: Int): Unit = {
    log(s"Signing requset $requestId")
    val keyName = getKeysSelectionElement().value
    log(s"Signing requset $requestId with key $keyName")

    backgroundAPI.signRequestWithKey(requestId, keyName).onComplete {
      case Success(()) =>
        loadRequests()
      case Failure(ex) =>
        log(s"Failed signing request ${requestId}: ${ex.getMessage}")
        throw ex
    }

  }

  def loadKeys(): Unit = {
    backgroundAPI
      .listKeys()
      .onComplete {
        case Success(keys) =>
          val keySelection = getKeysSelectionElement()
          while (keySelection.hasChildNodes()) {
            keySelection.removeChild(keySelection.firstChild)
          }

          for (keyName <- keys.names) {
            val opt = dom.document.createElement("option")
            opt.setAttribute("value", keyName)
            opt.textContent = keyName
            keySelection.appendChild(opt)
          }
        case Failure(ex) =>
          log(s"Failed obtaining keys list: ${ex.getMessage}")
          throw ex
      }
  }

  def getWalletStatus(): Unit = {
    backgroundAPI.getWalletStatus().onComplete {
      case Success(walletStatus) =>
        log(s"Got wallet status: ${walletStatus.status}")
      case Failure(ex) =>
        log(s"Failed obtaining wallet status: ${ex.getMessage}")
        throw ex
    }
  }

  def createWallet(): Unit = {
    backgroundAPI.createWallet(WalletManager.FIXME_WALLET_PASSWORD, Role.Verifier, "IOHK", Array()).map { _ =>
      log("Getting wallet status after creating it")
      getWalletStatus()
    }
  }

  def unlockWallet(): Unit = {
    backgroundAPI.unlockWallet(WalletManager.FIXME_WALLET_PASSWORD).map { _ =>
      dom.window.location.href = "popup.html"
      getWalletStatus()
    }
  }

  def lockWallet(): Unit = {
    backgroundAPI.lockWallet().map { _ =>
      dom.window.location.href = "popup-locked.html"
      getWalletStatus()
    }
  }

  def run(): Unit = {
    log("This was run by the popup script")

    val closeButton = dom.document.getElementById("close-button")

    val generateBtn = document.getElementById("generate").asInstanceOf[HTMLHeadElement]
    generateBtn.addEventListener("click", (ev: Event) => generate(), true)

    log("Getting wallet status from the popup script")
    getWalletStatus()
    if (closeButton != null) {
      loadRequests()
      loadKeys()
      closeButton.addEventListener("click", (ev: Event) => lockWallet(), true)
    } else {
      val openButton = dom.document.getElementById("open-button")
      openButton.addEventListener("click", (ev: Event) => unlockWallet(), true)
    }

    testEc()
  }

  def testEc(): Unit = {
    val ec = new EC(CURVE_NAME)
    val keyPair = ec.genKeyPair()

    val sk = keyPair.getPrivate()
    println("Private key (hex): " + keyPair.getPrivate("hex"))
    println("Private key (hex): " + sk.toString("hex"))
    println("Private key (decimal): " + sk.toString(10))
    println("Private key (decimal, aligned): " + sk.toString(10, 100))

    val keyPair2 = ec.keyFromPrivate(keyPair.getPrivate("hex"), "hex")
    println("Private key after ser/deser (hex): " + keyPair2.getPrivate("hex"))

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

  private def generate() = {
    console.info("**************************generate*****************************")

    val h2 = dom.document.getElementById("h2").asInstanceOf[typings.std.HTMLHeadingElement]
    val mnemonic = bip39.generateMnemonic()

    h2.innerText = mnemonic

  }

}

object Runner {

  def apply()(implicit ec: ExecutionContext): Runner = {
    val messages = new I18NMessages
    val backgroundAPI = new BackgroundAPI()
    new Runner(messages, backgroundAPI)
  }
}
