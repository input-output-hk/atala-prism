package io.iohk.atala.cvp.webextension.popup

import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import io.iohk.atala.cvp.webextension.background.wallet.WalletManager
import io.iohk.atala.cvp.webextension.common.Mnemonic
import org.scalajs.dom.html.{Div, Input}
import scalatags.JsDom.all.{div, _}
import typings.std.console

class RecoveryView(backgroundAPI: BackgroundAPI) {

  def recover(divElement: Div) = {
    console.info("**************************recover*****************************")

    val seedPhraseInput =
      input(cls := "_input", id := "seedPhrase", `type` := "text", placeholder := "12-word seed phrase").render
    val recoverWalletDiv = div(
      cls := "div__btn",
      id := "recoverWallet",
      "Recover wallet",
      onclick := { () =>
        recoverWallet(seedPhraseInput)
      }
    ).render

    val recover = {
      div(
        cls := "div__btngroup",
        div(
          cls := "div__passphrase",
          label(
            cls := "_label",
            "Enter Passphrase:",
            div(
              cls := "input__container",
              seedPhraseInput,
              div(cls := "input__container", label(cls := "_label_update", id := "walletStatus"))
            )
          )
        ),
        div(cls := "div__btngroup", recoverWalletDiv)
      )
    }.render

    divElement.innerHTML = ""
    divElement.appendChild(recover)
  }

  private def recoverWallet(seedPhrase: Input): Unit = {
    val mnemonic = Mnemonic(seedPhrase.value)
    backgroundAPI.recoverWallet(WalletManager.FIXME_WALLET_PASSWORD, mnemonic)
  }
}

object RecoveryView {
  def apply(backgroundAPI: BackgroundAPI): RecoveryView =
    new RecoveryView(backgroundAPI)
}
