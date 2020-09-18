package io.iohk.atala.cvp.webextension.popup

import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import io.iohk.atala.cvp.webextension.popup.models.View
import io.iohk.atala.cvp.webextension.popup.models.View.Main
import slinky.core._
import slinky.core.annotations.react
import slinky.core.facade.ReactElement
import slinky.web.html.{value, _}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

@react class SlinkyUnlockWalletView extends Component {

  case class Props(backgroundAPI: BackgroundAPI, switchToView: View => Unit)

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

  override def render: ReactElement = {

    div(id := "unlockScreen")(
      div(
        className := "div_logo",
        id := "logoPrism",
        img(src := "/assets/images/prism-logo.svg")
      ),
      h1(
        className := "h1_title",
        id := "h1_title",
        "Welcome Back"
      ),
      h3(className := "h3_title", id := "h3_title", "Please Unlock your account"),
      p(
        className := "description",
        id := "description",
        "For safety your account locks after a while. Please insert your password to unlock."
      ),
      p(
        className := "img_unlock",
        id := "img_unlock",
        img(src := "/assets/images/img-wallet-register.svg")
      ),
      p(
        className := "h4_unlock",
        id := "h4_unlock",
        "Insert your password"
      ),
      div(className := "div__field_group")(
        label(className := "label_unlock")("Password: "),
        div(className := "input__container")(
          input(
            id := "password",
            className := "_input",
            `type` := "password",
            placeholder := "Enter Password",
            value := state.password,
            onChange := (e => setPassword(e.target.value))
          )
        )
      ),
      div(className := "status_container")(
        div(className := "input__container")(
          state.message
        )
      ),
      div(className := "div__field_group")(
        div(
          id := "unlockButton",
          className := "btn_register",
          onClick := { () =>
            unlockWallet()
          }
        )("Unlock your account")
      ),
      p(
        className := "h4_forgot",
        id := "h4_forgot",
        "Forgot your password?"
      ),
      p(
        className := "h4_recover_account",
        id := "h4_recover_account",
        "Recover your account"
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
}
