package io.iohk.atala.cvp.webextension.popup

import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import io.iohk.atala.cvp.webextension.background.wallet.WalletStatus
import io.iohk.atala.cvp.webextension.popup.models.View
import io.iohk.atala.cvp.webextension.popup.models.View._
import org.scalajs.dom.experimental.URLSearchParams
import org.scalajs.dom.{console, window}
import slinky.core.Component
import slinky.core.annotations.react
import slinky.core.facade.ReactElement

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

@react class WalletView extends Component {
  case class Props(
      backgroundAPI: BackgroundAPI,
      blockchainExplorerUrl: String,
      termsUrl: String,
      privacyPolicyUrl: String
  )
  case class State(walletStatus: WalletStatus, view: View)

  override def componentDidMount(): Unit = {
    val paramValue = Option(new URLSearchParams(window.location.search).get("view"))
    val view = paramValue.flatMap(View.withNameInsensitiveOption).getOrElse(Default)
    props.backgroundAPI.getWalletStatus().onComplete {
      case Success(walletStatus) =>
        console.log(s"Wallet status: ${walletStatus.status} , view=$view")
        setState(state.copy(walletStatus.status, view))
      case Failure(ex) =>
        console.log(s"Failed obtaining wallet status: ${ex.getMessage}")
    }
  }

  override def initialState: State = {
    console.log(s"Wallet initialState status")
    State(WalletStatus.Missing, Default)
  }

  def updateView(view: View): Unit = {
    props.backgroundAPI.getWalletStatus().onComplete {
      case Success(walletStatus) =>
        console.log(s"Wallet status: ${walletStatus.status} , view=$view")
        setState(state.copy(walletStatus.status, view))
      case Failure(ex) =>
        console.log(s"Failed obtaining wallet status: ${ex.getMessage}")
    }
  }

  override def render: ReactElement = {
    (state.walletStatus, state.view) match {
      case (WalletStatus.Missing, Default) =>
        SlinkyInitialWalletView(props.backgroundAPI, (view: View) => updateView(view))
      case (WalletStatus.Missing, Register) =>
        SlinkyRegisterWalletView(
          props.backgroundAPI,
          props.termsUrl,
          props.privacyPolicyUrl,
          (view: View) => updateView(view)
        )
      case (WalletStatus.Missing | WalletStatus.Locked, Recover) =>
        SlinkyRecoverWalletView(
          props.backgroundAPI,
          props.termsUrl,
          props.privacyPolicyUrl,
          (view: View) => updateView(view)
        )
      case (WalletStatus.Unlocked, Register) =>
        SlinkyWelcomeRegisterView(props.backgroundAPI, props.blockchainExplorerUrl, (view: View) => updateView(view))
      case (WalletStatus.Unlocked, Recover) =>
        SlinkyWelcomeRecoverView(props.backgroundAPI, (view: View) => updateView(view))
      case (WalletStatus.Locked, _) =>
        SlinkyUnlockWalletView(props.backgroundAPI, (view: View) => updateView(view))
      case (WalletStatus.Unlocked, _) =>
        SlinkyMainWalletView(props.backgroundAPI, (view: View) => updateView(view))

      case _ =>
        SlinkyInitialWalletView(props.backgroundAPI, (view: View) => updateView(view))
    }
  }
}
