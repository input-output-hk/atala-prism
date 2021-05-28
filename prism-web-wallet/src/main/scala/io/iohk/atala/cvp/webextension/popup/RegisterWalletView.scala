package io.iohk.atala.cvp.webextension.popup

import com.alexitc.materialui.facade.materialUiCore.{materialUiCoreStrings, components => mui}
import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import io.iohk.atala.cvp.webextension.common.Mnemonic
import io.iohk.atala.cvp.webextension.popup.components.{ErrorMessage, PasswordInput}
import io.iohk.atala.cvp.webextension.popup.models.View.{Default, VerifyMnemonic}
import io.iohk.atala.cvp.webextension.popup.models.{Data, View}
import slinky.core._
import slinky.core.annotations.react
import slinky.core.facade.ReactElement
import slinky.web.html.{p, _}

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
      message: Option[String]
  )

  private val passwordRegex: String => Boolean = _.length >= 30

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
    State(Mnemonic(), "", "", None)
  }

  override def render(): ReactElement = {

    div(id := "registrationScreen", className := "generalContainer")(
      div(className := "div_logo", id := "logoPrism", img(src := "/assets/images/prism-logo.svg")),
      div(
        className := "elementWrapper",
        mui
          .Button(
            div(className := "backArrow", onClick := { () => props.switchToView(Default) })(
              img(className := "leftArrow", src := "/assets/images/arrow-l.svg"),
              p("Back")
            )
          )
          .className("muiButton")
          .size(materialUiCoreStrings.small),
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
        div(className := "descriptionContainer")(
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
        div(className := "")(
          p(
            className := "description",
            id := "description1",
            "Your password should be a memorable passphrase of at least 30 characters."
          ),
          div(
            className := "recoverPasswordContainer",
            PasswordInput(
              "Password",
              "Enter Password",
              state.password,
              password => setPassword(password),
              Some(_ => isValidPassphrase())
            )
          ),
          div(
            className := "recoverPasswordContainer",
            PasswordInput(
              "Confirm password",
              "Re-enter Password",
              state.password2,
              password => setPassword2(password),
              Some(_ => isPassphraseMatched())
            )
          ),
          state.message.map(ErrorMessage(_)),
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
    )
  }

  private def next(): Unit = {
    if (isValidInput()) {
      val data = Data(state.mnemonic, state.password)
      props.switchToView(VerifyMnemonic(data))
    }
  }

  def isValidPassphrase(): Boolean = {
    if (state.password.trim.isEmpty) {
      setState(_.copy(message = Some("Password cannot be empty.")))
      false
    } else if (!passwordRegex(state.password)) {
      setState(
        _.copy(message = Some("The password should be a memorable passphrase with minimum total characters of 30."))
      )
      false
    } else {
      setState(_.copy(message = None))
      true
    }
  }

  def isPassphraseMatched(): Boolean = {
    if (state.password != state.password2) {
      setState(_.copy(message = Some("The passwords do not match. Please try again.")))
      false
    } else {
      setState(_.copy(message = None))
      true
    }
  }

  private def isValidInput(): Boolean = {
    isValidPassphrase() && isPassphraseMatched()
  }
}
