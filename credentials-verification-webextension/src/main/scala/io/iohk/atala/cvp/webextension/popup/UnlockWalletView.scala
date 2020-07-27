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

    val unlockDiv = {
      div(id := "unlockScreen")(
        div(cls := "div__field_group")(
          label(cls := "_label")("Password: "),
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
            cls := "div__btn",
            onclick := { () =>
              unlockWallet(passwordInput, statusLabel, divElement)
            }
          )("Unlock wallet")
        )
      )
    }

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
