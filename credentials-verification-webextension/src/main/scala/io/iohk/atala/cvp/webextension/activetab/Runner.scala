package io.iohk.atala.cvp.webextension.activetab

import io.iohk.atala.cvp.webextension.Config
import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import io.iohk.atala.cvp.webextension.common.I18NMessages

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class Runner(config: ActiveTabConfig, backgroundAPI: BackgroundAPI, messages: I18NMessages)(implicit
    ec: ExecutionContext
) {

  def run(): Unit = {
    log("This was run by the active tab")
    backgroundAPI.requestSignature("deadbeef").onComplete {
      case Success(signature) =>
        log(s"Signature delivered to tab: ${signature.signature}")
        backgroundAPI.sendBrowserNotification(messages.appName, "Message signed and delivered to tab")
      case Failure(ex) =>
        log(s"Error from signing delivered to tab: ${ex.getMessage}")
    }
  }

  private def log(msg: String): Unit = {
    println(s"activeTab: $msg")
  }
}

object Runner {

  def apply(config: Config)(implicit ec: ExecutionContext): Runner = {
    val backgroundAPI = new BackgroundAPI
    val messages = new I18NMessages
    new Runner(config.activeTabConfig, backgroundAPI, messages)
  }
}
