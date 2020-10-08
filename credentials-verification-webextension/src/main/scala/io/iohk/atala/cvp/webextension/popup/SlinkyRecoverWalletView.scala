package io.iohk.atala.cvp.webextension.popup

import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import io.iohk.atala.cvp.webextension.common.Mnemonic
import io.iohk.atala.cvp.webextension.popup.models.View
import io.iohk.atala.cvp.webextension.popup.models.View.Recover
import slinky.core._
import slinky.core.annotations.react
import slinky.core.facade.ReactElement
import slinky.web.html.{value, _}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

@react class SlinkyRecoverWalletView extends Component {
  case class Props(backgroundAPI: BackgroundAPI, switchToView: View => Unit)
  case class State(
      seed: String,
      password: String,
      password2: String,
      message: String
  )

  private def setPassword(newValue: String): Unit = {
    setState(_.copy(password = newValue))
  }

  private def setPassword2(newValue: String): Unit = {
    setState(_.copy(password2 = newValue))
  }
  private def setSeedPhrase(newValue: String): Unit = {
    setState(_.copy(seed = newValue))
  }
  override def initialState: State = {
    State("", "", "", "")
  }

  override def render: ReactElement = {

    div(id := "recoveryScreen")(
      h3(className := "h3_recover")("Recover your wallet"),
      div(className := "div__field_group")(
        h4(className := "h4_recover", id := "h4_recover", "Type your recovery phrase"),
        div(className := "input__container")(
          input(
            id := "seedphrase",
            className := "seedPhraseContainer",
            placeholder := "12-words",
            value := state.seed,
            onChange := (e => setSeedPhrase(e.target.value))
          )
        )
      ),
      div(className := "div__field_group")(
        h4(className := "h4_enter_pass", id := "h4_recover", "Enter a new password and confirm it"),
        label(className := "_label")("Password"),
        div(className := "input__container")(
          input(
            id := "password",
            className := "_input",
            `type` := "password",
            placeholder := "Enter password",
            value := state.password,
            onChange := (e => setPassword(e.target.value))
          )
        )
      ),
      div(className := "div__field_group")(
        label(className := "_label")("Confirm password"),
        div(className := "input__container")(
          input(
            id := "password2",
            className := "_input",
            `type` := "password",
            placeholder := "Re-enter password",
            value := state.password2,
            onChange := (e => setPassword2(e.target.value))
          )
        )
      ),
      div(className := "status_container")(
        label(className := "_label_update")(state.message)
      ),
      div(className := "div__field_group")(
        div(
          id := "recoverButton",
          className := "btn_verify",
          onClick := { () =>
            recoverWallet()
          }
        )("Recover wallet")
      )
    )
  }

  private def recoverWallet(): Unit = {
    if (isValidInput(state)) {
      props.backgroundAPI
        .recoverWallet(state.password, Mnemonic(state.seed))
        .onComplete {
          case Success(_) => props.switchToView(Recover)
          case Failure(ex) =>
            setState(state.copy(message = "Failed recovering wallet"))
            println(s"Failed recovering wallet : ${ex.getMessage}")
        }
    }
  }

  private def isValidInput(state: State): Boolean = {
    if (state.password.isEmpty) {
      setState(state.copy(message = "Password cannot be empty"))
      false
    } else if (state.password != state.password2) {
      setState(state.copy(message = "Password verification does not match"))
      false
    } else if (!Mnemonic.isValid(state.seed)) {
      setState(state.copy(message = "Invalid Seed Phrase"))
      false
    } else {
      setState(state.copy(message = ""))
      true
    }
  }
}
