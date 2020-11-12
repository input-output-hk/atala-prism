package io.iohk.atala.cvp.webextension.popup

import io.iohk.atala.cvp.webextension.Config
import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import io.iohk.atala.cvp.webextension.common.I18NMessages
import org.scalajs.dom
import slinky.web.ReactDOM

import scala.concurrent.ExecutionContext

class Runner(
    messages: I18NMessages,
    backgroundAPI: BackgroundAPI,
    blockchainExplorerUrl: String,
    termsUrl: String,
    privacyPolicyUrl: String
)(implicit
    ec: ExecutionContext
) {

  def run(): Unit = {
    dom.window.onload = _ => {
      ReactDOM.render(
        WalletView(backgroundAPI, blockchainExplorerUrl, termsUrl, privacyPolicyUrl),
        dom.document.getElementById("main-container")
      )
    }
    dom.window.onfocus = _ => {
      ReactDOM.render(
        WalletView(backgroundAPI, blockchainExplorerUrl, termsUrl, privacyPolicyUrl),
        dom.document.getElementById("main-container")
      )
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
