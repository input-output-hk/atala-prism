package io.iohk.atala.cvp.webextension.popup

import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import org.scalajs.dom.html.{Div, Input, Label}
import scalatags.JsDom.all.{div, _}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

class UnlockWalletView(backgroundAPI: BackgroundAPI)(implicit ec: ExecutionContext) {

  val status: Promise[Unit] = Promise[Unit]()

  def unlock(divElement: Div) = {
    val passwordInput =
      input(id := "password", cls := "_input", `type` := "password", placeholder := "Password").render
    val statusLabel: Label = label(cls := "_label_update")("").render

    val unlockDiv =
      div(id := "unlockScreen")(
        div(
          cls := "div_logo",
          id := "logoPrism",
          img(src := "/assets/images/prism-logo.svg")
        ),
        h1(
          cls := "h1_title",
          id := "h1_title",
          "Welcome Back"
        ),
        h3(cls := "h3_title", id := "h3_title", "Please Unlock your account"),
        p(
          cls := "description",
          id := "description",
          "For safety your account locks after a while. Please insert your password to unlock."
        ),
        p(
          cls := "img_unlock",
          id := "img_unlock",
          img(src := "/assets/images/img-wallet-register.svg")
        ),
        p(
          cls := "h4_unlock",
          id := "h4_unlock",
          "Insert your password"
        ),
        div(cls := "div__field_group")(
          label(cls := "label_unlock")("Password: "),
          div(cls := "input__container")(
            passwordInput
          )
        ),
        div(cls := "status_container")(
          div(cls := "input__container")(
            statusLabel
          )
        ),
        div(cls := "div__field_group")(
          div(
            id := "unlockButton",
            cls := "btn_register",
            onclick := { () =>
              unlockWallet(passwordInput, statusLabel, divElement)
            }
          )("Unlock your account")
        ),
        p(
          cls := "h4_forgot",
          id := "h4_forgot",
          "Forgot your password?"
        ),
        p(
          cls := "h4_recover_account",
          id := "h4_recover_account",
          "Recover your account"
        )
      )

    divElement.clear()
    divElement.appendChild(unlockDiv.render)
  }

  private def unlockWallet(
      passwordInput: Input,
      statusLabel: Label,
      divElement: Div
  ): Unit = {
    backgroundAPI
      .unlockWallet(passwordInput.value)
      .flatMap { _ =>
        MainWalletView(backgroundAPI).mainWalletScreen(divElement)
      }
      .onComplete {
        case Success(_) => ()
        case Failure(ex) =>
          statusLabel.textContent = "Invalid Password"
          println(s"Password Invalid : ${ex.getMessage}")
          ()
      }
  }
}

object UnlockWalletView {
  def apply(backgroundAPI: BackgroundAPI)(implicit ec: ExecutionContext): UnlockWalletView =
    new UnlockWalletView(backgroundAPI)
}
