package io.iohk.atala.cvp.webextension.popup

import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import org.scalajs.dom.html.{Body, Div}
import scalatags.JsDom.all.{div, script, img, _}

import scala.concurrent.ExecutionContext

class InitialWalletView(backgroundAPI: BackgroundAPI)(implicit ec: ExecutionContext) {
  def htmlBody: Body = {
    val containerDiv: Div = div(
      cls := "container",
      id := "containerId"
    ).render

    lazy val logoDiv =
      div(
        cls := "div_logo",
        id := "logoPrism",
        img(src := "/assets/images/prism-logo.svg")
      ).render

    lazy val titleContainer =
      div(
        cls := "title_container",
        id := "title_container"
      ).render

    lazy val h3Div = h3(cls := "h3_title", id := "h3_title", "Welcome to your").render

    lazy val h1Div =
      h1(
        cls := "h1_title",
        id := "h1_title",
        "Prism Browser Wallet"
      ).render

    lazy val descriptionDiv =
      p(
        cls := "description",
        id := "description",
        "Register now to your browser wallet or recover your account."
      ).render

    lazy val imageDiv =
      p(
        cls := "div_img",
        id := "div_img",
        img(src := "/assets/images/img-wallet-register.svg")
      ).render

    titleContainer.appendChild(logoDiv)
    titleContainer.appendChild(h3Div)
    titleContainer.appendChild(h1Div)
    titleContainer.appendChild(descriptionDiv)
    titleContainer.appendChild(imageDiv)
    containerDiv.appendChild(titleContainer)

    lazy val registrationDiv =
      div(
        cls := "btn_register",
        id := "registrationScreenButton",
        "Register",
        onclick := { () =>
          RegistrationView(backgroundAPI).registrationScreen(containerDiv)
        }
      ).render

    lazy val recoverDiv =
      div(
        cls := "btn_recover",
        id := "recoveryScreenButton",
        "Recover your account",
        onclick := { () =>
          RecoveryView(backgroundAPI).recover(containerDiv)
        }
      ).render

    lazy val unlockWalletDiv =
      div(
        cls := "div__btn",
        id := "unlockScreenButton",
        "Unlock Wallet",
        onclick := { () =>
          UnlockWalletView(backgroundAPI).unlock(containerDiv)
        }
      ).render

    containerDiv.appendChild(registrationDiv)
    containerDiv.appendChild(recoverDiv)
    containerDiv.appendChild(unlockWalletDiv)

    val htmlBody = body(
      link(
        rel := "stylesheet",
        href := "https://fonts.googleapis.com/css2?family=Source+Sans+Pro:wght@400;600&display=swap"
      ),
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
  def apply(backgroundAPI: BackgroundAPI)(implicit ec: ExecutionContext): InitialWalletView =
    new InitialWalletView(backgroundAPI)
}
