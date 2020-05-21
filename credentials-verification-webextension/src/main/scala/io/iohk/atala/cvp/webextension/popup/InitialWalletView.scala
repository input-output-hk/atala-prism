package io.iohk.atala.cvp.webextension.popup

import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import org.scalajs.dom.html.{Body, Div}
import scalatags.JsDom.all.{div, script, _}

class InitialWalletView(backgroundAPI: BackgroundAPI) {
  def htmlBody: Body = {
    val containerDiv: Div = div(
      cls := "container",
      id := "containerId"
    ).render

    lazy val recoverDiv =
      div(
        cls := "div__btn",
        id := "recover",
        "Recover Wallet",
        onclick := { () =>
          RecoveryView(backgroundAPI).recover(containerDiv)
        }
      ).render

    lazy val registrationDiv =
      div(
        cls := "div__btn",
        id := "registrationScreen",
        "Register",
        onclick := { () =>
          RegistrationView(backgroundAPI).registrationScreen(containerDiv)
        }
      ).render

    containerDiv.appendChild(recoverDiv)
    containerDiv.appendChild(registrationDiv)
    val htmlBody = body(
      link(rel := "stylesheet", href := "css/popup.css"),
      script(src := "scripts/common.js"),
      script(src := "main-bundle.js"),
      script(src := "scripts/popup-script.js"),
      containerDiv.render
    ).render
    htmlBody
  }
}
object InitialWalletView {
  def apply(backgroundAPI: BackgroundAPI): InitialWalletView =
    new InitialWalletView(backgroundAPI)
}
