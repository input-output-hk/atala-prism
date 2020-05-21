package io.iohk.atala.cvp.webextension.popup

import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import io.iohk.atala.cvp.webextension.background.wallet.{Role, WalletManager}
import io.iohk.atala.cvp.webextension.common.Mnemonic
import org.scalajs.dom.Event
import org.scalajs.dom.raw.{HTMLDivElement, HTMLInputElement, HTMLLabelElement}
import typings.std.{console, document}

class RecoveryView(backgroundAPI: BackgroundAPI) {

  //TODO When working on recovery story
  def recover(divElement: HTMLDivElement) = {
    console.info("**************************recover*****************************")
    divElement.innerHTML =
      """<div class="div__btngroup">
        |<div class="div__passphrase">
        |<label class="_label">Enter Passphrase: <div class="input__container">
        |<input class="_input" id="passphraseField" type="text" placeholder="12-word passphrase" autocorrect="off" autocapitalize="off" value="">
        |<div class="input__container"><label class="_label_update" id="walletStatus">
        |</div>
        |</label>
        |</div>
        |</div>
        |<div class="div__btngroup">
        |<div class="div__btn" id="openWallet">
        | Open wallet</div>
        |</div>""".stripMargin

    val openWallet =
      document.getElementById("openWallet").asInstanceOf[HTMLDivElement]

    openWallet.addEventListener("click", (ev: Event) => create(), true)

  }

  private def create(): Unit = {
    val seed: HTMLInputElement =
      document.getElementById("passphraseField").asInstanceOf[HTMLInputElement]
    val mnemonic = Mnemonic(seed.value)
    backgroundAPI.createWallet(WalletManager.FIXME_WALLET_PASSWORD, mnemonic, Role.Verifier, "IOHK", Array())
    val status =
      document.getElementById("walletStatus").asInstanceOf[HTMLLabelElement]
    status.textContent = "Wallet Created"
  }
}

object RecoveryView {
  def apply(backgroundAPI: BackgroundAPI): RecoveryView =
    new RecoveryView(backgroundAPI)
}
