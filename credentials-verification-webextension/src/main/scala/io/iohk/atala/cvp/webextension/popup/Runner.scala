package io.iohk.atala.cvp.webextension.popup

import io.iohk.atala.cvp.webextension.Config
import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import io.iohk.atala.cvp.webextension.common.I18NMessages
import org.scalajs.dom
import slinky.web.ReactDOM

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class Runner(messages: I18NMessages, backgroundAPI: BackgroundAPI)(implicit ec: ExecutionContext) {

  def run(): Unit = {
    dom.window.onload = _ => {
      ReactDOM.render(WalletView(backgroundAPI), dom.document.getElementById("root"))
    }
  }

}

object Runner {
  def apply(config: Config)(implicit ec: ExecutionContext): Runner = {
    val messages = new I18NMessages
    val backgroundAPI = new BackgroundAPI()
    new Runner(messages, backgroundAPI)
  }
}
