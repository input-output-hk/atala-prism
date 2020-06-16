package io.iohk.atala.cvp.webextension.popup

import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import io.iohk.atala.cvp.webextension.common.Mnemonic
import io.iohk.atala.cvp.webextension.popup.utils.ValidationUtils
import org.scalajs.dom.html.{Div, Input, Label}
import scalatags.JsDom.all.{div, _}
import typings.std.console

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class RecoveryView(backgroundAPI: BackgroundAPI)(implicit ec: ExecutionContext) {

  def recover(divElement: Div) = {
    console.info("**************************recover*****************************")

    val seedPhraseInput =
      input(id := "seedphrase", cls := "_input", placeholder := "12-word seed phrase").render
    val passwordInput =
      input(id := "password", cls := "_input", `type` := "password", placeholder := "Password").render
    val password2Input =
      input(id := "password2", cls := "_input", `type` := "password", placeholder := "Password verification").render
    val statusLabel: Label = label(cls := "_label_update")("").render

    val recover = {
      div(id := "recoveryScreen")(
        div(cls := "div__field_group")(
          label(cls := "_label")("Seed phrase: "),
          div(cls := "input__container")(
            seedPhraseInput
          )
        ),
        div(cls := "div__field_group")(
          label(cls := "_label")("Password: "),
          div(cls := "input__container")(
            passwordInput
          )
        ),
        div(cls := "div__field_group")(
          label(cls := "_label")("Verify Password: "),
          div(cls := "input__container")(
            password2Input
          )
        ),
        div(cls := "status_container")(
          div(cls := "input__container")(
            statusLabel
          )
        ),
        div(cls := "div__field_group")(
          div(
            id := "recoverButton",
            cls := "div__btn",
            onclick := { () =>
              recoverWallet(seedPhraseInput, passwordInput, password2Input, statusLabel, divElement)
            }
          )("Recover wallet")
        )
      )
    }

    divElement.clear()
    divElement.appendChild(recover.render)
  }

  private def recoverWallet(
      seedPhrase: Input,
      passwordInput: Input,
      password2Input: Input,
      statusLabel: Label,
      divElement: Div
  ): Unit = {
    ValidationUtils.checkPasswordErrors(passwordInput, password2Input) match {
      case Some(errors) => statusLabel.textContent = errors
      case None =>
        val mnemonic = Mnemonic(seedPhrase.value)
        backgroundAPI
          .recoverWallet(passwordInput.value, mnemonic)
          .flatMap { _ =>
            MainWalletView(backgroundAPI).mainWalletScreen(divElement)
          }
          .onComplete {
            case Failure(ex) =>
              println(s"Failed recovering wallet : ${ex.getMessage}")
              throw ex
          }
    }
  }
}

object RecoveryView {
  def apply(backgroundAPI: BackgroundAPI)(implicit ec: ExecutionContext): RecoveryView =
    new RecoveryView(backgroundAPI)
}
