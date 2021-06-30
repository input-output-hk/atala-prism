package io.iohk.atala.cvp.webextension.popup

import com.alexitc.materialui.facade.materialUiCore.{materialUiCoreStrings, components => mui}
import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import io.iohk.atala.cvp.webextension.common.Mnemonic
import io.iohk.atala.cvp.webextension.popup.components.{ChipInput, ErrorMessage, PasswordInput, ProgressButton}
import io.iohk.atala.cvp.webextension.popup.models.View
import io.iohk.atala.cvp.webextension.popup.models.View.{Default, Recover}
import slinky.core._
import slinky.core.annotations.react
import slinky.core.facade.ReactElement
import slinky.web.html.{onChange, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

@react class RecoverWalletView extends Component {

  case class Props(
      backgroundAPI: BackgroundAPI,
      termsUrl: String,
      privacyPolicyUrl: String,
      switchToView: (View) => Unit
  )

  case class State(
      password: String,
      password2: String,
      message: Option[String],
      tandc: Boolean,
      isLoading: Boolean,
      value: String,
      var values: Seq[String] = Nil
  ) {
    val seed = this.values.mkString(" ")
  }

  private val passwordRegex: String => Boolean = _.length >= 30

  override def initialState: State = {
    State("", "", None, false, false, "", Nil)
  }

  private def setChips(values: Seq[String]): Unit = {
    setState(_.copy(values = values))
  }

  override def render(): ReactElement = {

    val enableButton = state.tandc

    div(id := "recoveryScreen", className := "generalContainer")(
      div(className := "div_logo", id := "logoPrism", img(src := "/assets/images/prism-logo.svg")),
      div(
        className := "elementWrapper",
        div(
          mui
            .Button(
              div(className := "backArrow", onClick := { () => props.switchToView(Default) })(
                img(className := "leftArrow", src := "/assets/images/arrow-l.svg"),
                p("Back")
              )
            )
            .className("muiButton")
            .size(materialUiCoreStrings.small),
          h3(className := "h3_recover")("Recover your wallet"),
          ChipInput(chips => setChips(chips), Some(_ => isValidSeedPhrase())),
          h4(className := "h4_enter_pass", id := "h4_recover", "Enter a new password and confirm it"),
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
          div(className := "checkboxContainer")(
            div(className := "input__container")(
              div()(
                input(
                  id := "tandc",
                  `type` := "checkbox",
                  onChange := (e => setTandC(e.currentTarget.checked))
                ),
                label(className := "_label_txt", htmlFor := "tandc")(
                  div(
                    className := "paddingLeft",
                    "Accept",
                    a(
                      href := s"${props.termsUrl}",
                      target := "_blank",
                      className := "_label_link"
                    )(
                      "Terms and Conditions"
                    ),
                    "and",
                    a(
                      href := s"${props.privacyPolicyUrl}",
                      target := "_blank",
                      className := "_label_link"
                    )(
                      "Privacy Policy Agreement"
                    )
                  )
                )
              )
            )
          )
        ),
        state.message.map(ErrorMessage(_)),
        div(
          className := "btn-Container",
          ProgressButton(
            "Recover wallet",
            enableButton,
            state.isLoading,
            _ => recoverWallet()
          )
        )
      )
    )
  }

  private def setPassword(newValue: String): Unit = {
    setState(_.copy(password = newValue))
  }

  private def setPassword2(newValue: String): Unit = {
    setState(_.copy(password2 = newValue))
  }

  private def setTandC(newValue: Boolean): Unit = {
    setState(_.copy(tandc = newValue))
  }

  private def recoverWallet(): Unit = {
    if (isValidInput()) {
      setState(_.copy(isLoading = true))
      props.backgroundAPI
        .recoverWallet(state.password, Mnemonic(state.seed))
        .onComplete {
          case Success(_) => props.switchToView(Recover)
          case Failure(ex) =>
            setState(_.copy(isLoading = false, message = Some("Failed recovering wallet")))
            println(s"Failed recovering wallet : ${ex.getMessage}")
        }
    }
  }

  def isValidPassphrase(): Boolean = {
    if (state.password.trim.isEmpty) {
      setState(_.copy(message = Some("Passphrase cannot be empty.")))
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

  def isValidSeedPhrase(): Boolean = {
    if (!Mnemonic.isValid(state.seed)) {
      setState(_.copy(message = Some("Invalid Seed Phrase.")))
      false
    } else {
      setState(_.copy(message = None))
      true
    }
  }

  private def isValidInput(): Boolean = {
    isValidSeedPhrase() &&
    isValidPassphrase() &&
    isPassphraseMatched()
  }

}
