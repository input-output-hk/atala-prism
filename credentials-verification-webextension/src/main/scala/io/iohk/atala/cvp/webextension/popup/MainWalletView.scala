package io.iohk.atala.cvp.webextension.popup

import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import org.scalajs.dom.html.{Div, Label}
import org.scalajs.dom.raw.Node
import scalatags.JsDom.all.{div, id, _}

import scala.concurrent.{ExecutionContext, Future}

class MainWalletView(backgroundAPI: BackgroundAPI)(implicit ec: ExecutionContext) {

  def mainWalletScreen(divElement: Div): Future[Node] = {

    val statusLabel: Label = label(cls := "_label_update")("").render

    val mainScreen = {
      div(cls := "status_container", id := "mainView")(
        div(cls := "input__container")(
          statusLabel
        )
      )
    }.render

    backgroundAPI.getWalletStatus().map { walletStatus =>
      statusLabel.textContent = walletStatus.status.toString
      println(s"Got wallet status: ${walletStatus.status}")
      divElement.innerHTML = ""
      divElement.appendChild(mainScreen)
    }
  }
}

object MainWalletView {
  def apply(backgroundAPI: BackgroundAPI)(implicit ec: ExecutionContext): MainWalletView =
    new MainWalletView(backgroundAPI)
}
