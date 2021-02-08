package io.iohk.atala.cvp.webextension.popup

import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import io.iohk.atala.cvp.webextension.popup.components.PasswordInput
import io.iohk.atala.cvp.webextension.popup.models.View
import io.iohk.atala.cvp.webextension.popup.models.View.{Main, Recover}
import slinky.core._
import slinky.core.annotations.react
import slinky.core.facade.ReactElement
import slinky.web.html._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

@react class UnlockWalletView extends Component {

  case class Props(backgroundAPI: BackgroundAPI, switchToView: (View) => Unit)

  case class State(
      password: String,
      message: String
  )

  private def setPassword(newValue: String): Unit = {
    setState(_.copy(password = newValue))
  }

  override def initialState: State = {
    State("", "")
  }

  override def render(): ReactElement = {

    def error() = {
      if (state.message.nonEmpty) {
        div(className := "errorContainer")(
          label(className := "_label_update")(
            state.message,
            img(className := "errorImg", src := "/assets/images/error.svg")
          )
        )
      } else {
        div(className := "errorContainer")()
      }
    }

    div(id := "unlockScreen", className := "sidePadding")(
      div(
        className := "spaceBetween",
        div(
          className := "div_logo",
          id := "logoPrism",
          img(src := "/assets/images/prism-logo.svg")
        ),
        div(
          h1(
            className := "h1_title",
            id := "h1_title",
            "Welcome Back"
          ),
          p(
            className := "description",
            id := "description",
            "For safety reasons, your wallet is automatically locked after a period of inactivity.Please enter your password to unlock your wallet"
          ),
          div(
            className := "img_center",
            img(className := "img_unlock", id := "img_unlock", src := "/assets/images/img-wallet-register.svg")
          ),
          p(
            className := "h4_unlock",
            id := "h4_unlock",
            "Enter your password"
          ),
          PasswordInput("Password", "Enter Password", state.password, password => setPassword(password)),
          error()
        ),
        div(
          div(
            id := "unlockButton",
            className := "btn_register",
            onClick := { () =>
              unlockWallet()
            }
          )("Unlock your wallet"),
          div(
            className := "btn_recover",
            id := "unlockButton",
            "Recover your wallet",
            onClick := { () =>
              recoverWallet()
            }
          ),
          div(className := "div__field_group")(
            label(className := "forgotten-pass")(
              "If you have forgotten your password, you need to recover your wallet."
            )
          )
        )
      )
    )
  }

  private def unlockWallet(): Unit = {
    if (state.password.isEmpty) {
      setState(state.copy(message = "Password cannot be empty"))
    } else {
      setState(state.copy(message = ""))
      props.backgroundAPI
        .unlockWallet(state.password)
        .onComplete {
          case Success(_) => props.switchToView(Main)
          case Failure(ex) =>
            setState(state.copy(message = "Failed unlocking wallet"))
            println(s"Failed unlocking wallet : ${ex.getMessage}")
        }
    }
  }

  private def recoverWallet(): Unit = {
    props.switchToView(Recover)
  }
}
