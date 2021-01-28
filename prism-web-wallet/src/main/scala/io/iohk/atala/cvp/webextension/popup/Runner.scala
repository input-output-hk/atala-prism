package io.iohk.atala.cvp.webextension.popup

import io.iohk.atala.cvp.webextension.Config
import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import io.iohk.atala.cvp.webextension.common.I18NMessages
import org.scalajs.dom
import slinky.web.ReactDOM
import typings.std.global.window

import scala.concurrent.ExecutionContext

class Runner(
    messages: I18NMessages,
    backgroundAPI: BackgroundAPI,
    blockchainExplorerUrl: String,
    termsUrl: String,
    privacyPolicyUrl: String
) {

  def run(): Unit = {
    processMessages()
    dom.window.onload = _ => {
      ReactDOM.render(
        WalletView(backgroundAPI, blockchainExplorerUrl, termsUrl, privacyPolicyUrl),
        dom.document.getElementById("main-container")
      )
    }
  }

  private def processMessages() = {
    val myExtensionId = chrome.runtime.Runtime.id
    chrome.runtime.Runtime.onMessage
      .filter { message =>
        message.sender.id.getOrElse("id") == myExtensionId
      }
      .listen { message =>
        message.value.filter(_ == "reload").foreach(_ => window.location.reload(true))
      }
  }
}

object Runner {
  def apply(config: Config)(implicit ec: ExecutionContext): Runner = {
    val messages = new I18NMessages
    val backgroundAPI = new BackgroundAPI()
    new Runner(messages, backgroundAPI, config.blockchainExplorerUrl, config.termsUrl, config.privacyPolicyUrl)
  }
}
