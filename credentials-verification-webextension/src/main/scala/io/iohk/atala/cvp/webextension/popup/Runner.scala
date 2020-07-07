package io.iohk.atala.cvp.webextension.popup

import io.iohk.atala.cvp.webextension.Config
import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import io.iohk.atala.cvp.webextension.background.wallet.WalletStatus.Unlocked
import io.iohk.atala.cvp.webextension.common.I18NMessages
import org.scalajs.dom

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class Runner(messages: I18NMessages, backgroundAPI: BackgroundAPI)(implicit ec: ExecutionContext) {

  def getWalletStatus(): Unit = {
    backgroundAPI.getWalletStatus().onComplete {
      case Success(walletStatus) =>
        log(s"Got wallet status: ${walletStatus.status}")
      case Failure(ex) =>
        log(s"Failed obtaining wallet status: ${ex.getMessage}")
        throw ex
    }
  }

  def unlockWallet(password: String): Unit = {
    backgroundAPI.unlockWallet(password).map { _ =>
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
    dom.window.onload = _ => {
      backgroundAPI.getWalletStatus().onComplete {
        case Success(walletStatus) =>
          log(s"Got wallet status: ${walletStatus.status}")
          walletStatus.status match {
            case Unlocked => dom.document.body = MainWalletView(backgroundAPI).htmlBody
            case _ => dom.document.body = InitialWalletView(backgroundAPI).htmlBody
          }
        case Failure(ex) =>
          log(s"Failed obtaining wallet status: ${ex.getMessage}")
          throw ex
      }
    }
  }

  private def log(msg: String): Unit = {
    println(s"popup: $msg")
  }

}

object Runner {

  def apply(config: Config)(implicit ec: ExecutionContext): Runner = {
    val messages = new I18NMessages
    val backgroundAPI = new BackgroundAPI()
    new Runner(messages, backgroundAPI)
  }
}
