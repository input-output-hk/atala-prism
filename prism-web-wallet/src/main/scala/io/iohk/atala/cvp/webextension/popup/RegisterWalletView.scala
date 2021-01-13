package io.iohk.atala.cvp.webextension.popup

import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import io.iohk.atala.cvp.webextension.common.Mnemonic
import io.iohk.atala.cvp.webextension.popup.models.View.{Default, VerifyMnemonic}
import io.iohk.atala.cvp.webextension.popup.models.{Data, View}
import slinky.core._
import slinky.core.annotations.react
import slinky.core.facade.ReactElement
import slinky.web.html.{p, _}
import com.alexitc.materialui.facade.materialUiCore.{components => mui}
import com.alexitc.materialui.facade.materialUiIcons.{components => muiIcons}

@react class RegisterWalletView extends Component {

  case class Props(
      backgroundAPI: BackgroundAPI,
      termsUrl: String,
      privacyPolicyUrl: String,
      switchToView: (View) => Unit
  )

  case class State(
      mnemonic: Mnemonic,
      password: String,
      password2: String,
      message: String
  )

  private val mnemonicElement: ReactElement = div(className := "words_container")(
    ReactElement.iterableToElement {
      state.mnemonic.seed.split(" ").zipWithIndex.map {
        case (w, i) =>
          val word = s"${i + 1}. $w"
          div(className := "span_container", div(className := "span", span(word)))
      }
    }
  )

  private def setPassword(newValue: String): Unit = {
    setState(_.copy(password = newValue))
  }

  private def setPassword2(newValue: String): Unit = {
    setState(_.copy(password2 = newValue))
  }

  override def initialState: State = {
    State(Mnemonic(), "", "", "")
  }

  override def render(): ReactElement = {

    div(id := "registrationScreen", className := "status_container")(
      mui.IconButton.onClick(_ => props.switchToView(Default))(muiIcons.ArrowBackSharp()),
      h3(className := "h3_register", id := "h3_register", "Account Registration"),
      div(className := "div__field_group")(
        h4(className := "h4_register")("Save your recovery phrase"),
        p(
          className := "description",
          id := "description",
          "Your recovery phrase is your personal key that gives you access to your account."
        ),
        mnemonicElement
      ),
      div(className := "div__field_group")(
        p(
          className := "description",
          id := "description1",
          "Without it you will not be able to access any of your records or funds in case you lose your device or quite the app."
        ),
        p(
          className := "description",
          id := "description2",
          "Please store your recovery phrase securely before proceeding."
        )
      ),
      h4(className := "h4_register", id := "h4_register", "Wallet information"),
      div(className := "div__field_group")(
        label(className := "_label")("Password "),
        div(className := "input__container")(
          input(
            id := "password",
            className := "_input",
            `type` := "password",
            placeholder := "Enter password",
            value := state.password,
            onChange := (e => setPassword(e.target.value))
          )
        ),
        div(className := "div__field_group")(
          label(className := "_label")("Confirm password "),
          div(className := "input__container")(
            input(
              id := "password2",
              className := "_input",
              `type` := "password",
              placeholder := "Re-enter password",
              value := state.password2,
              onChange := (e => setPassword2(e.target.value)),
              onKeyDown := { e =>
                if (e.key == "Enter") {
                  next()
                }
              }
            )
          )
        ),
        div(className := "status_container")(
          div(className := "input__container")(
            label(className := "_label_update")(state.message)
          )
        ),
        div(className := "div__field_group")(
          div(
            id := "registerButton",
            className := "btn_register",
            onClick := { () =>
              next()
            }
          )("Next")
        )
      )
    )
  }

  private def next(): Unit = {
    if (isValidInput(state)) {
      val data = Data(state.mnemonic, state.password)
      props.switchToView(VerifyMnemonic(data))
    }
  }

  private def isValidInput(state: State): Boolean = {
    if (state.password.isEmpty) {
      setState(state.copy(message = "Password cannot be empty"))
      false
    } else if (state.password != state.password2) {
      setState(state.copy(message = "Password verification does not match"))
      false
    } else {
      setState(state.copy(message = ""))
      true
    }
  }
}
