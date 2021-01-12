package io.iohk.atala.cvp.webextension.popup

import com.alexitc.materialui.facade.materialUiCore.anon.PartialClassNameMapCircul
import com.alexitc.materialui.facade.materialUiCore.materialUiCoreStrings.indeterminate
import com.alexitc.materialui.facade.materialUiCore.{components => mui}
import com.alexitc.materialui.facade.materialUiIcons.{components => muiIcons}
import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import io.iohk.atala.cvp.webextension.common.Mnemonic
import io.iohk.atala.cvp.webextension.popup.models.View
import io.iohk.atala.cvp.webextension.popup.models.View.{Default, Recover}
import slinky.core._
import slinky.core.annotations.react
import slinky.core.facade.ReactElement
import slinky.web.html._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

@react class SlinkyRecoverWalletView extends Component {
  case class Props(backgroundAPI: BackgroundAPI, termsUrl: String, privacyPolicyUrl: String, switchToView: View => Unit)
  case class State(
      seed: String,
      password: String,
      password2: String,
      message: String,
      tandc: Boolean = false,
      privacyPolicy: Boolean = false,
      isLoading: Boolean = false
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

  private def setTandC(newValue: Boolean): Unit = {
    setState(_.copy(tandc = newValue))
  }

  private def setPrivacyPolicy(newValue: Boolean): Unit = {
    setState(_.copy(privacyPolicy = newValue))
  }

  def enableButton = {
    if (state.tandc && state.privacyPolicy && !state.isLoading) {
      className := "btn_verify"
    } else {
      className := "btn_verify disabled"
    }
  }

  override def render(): ReactElement = {

    div(id := "recoveryScreen", className := "status_container_recover")(
      mui.IconButton.onClick(_ => props.switchToView(Default))(muiIcons.ArrowBackSharp()),
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
      div(className := "div__field_group")(
        div(className := "input__container")(
          div()(
            input(
              id := "tandc",
              `type` := "checkbox",
              onChange := (e => setTandC(e.currentTarget.checked))
            ),
            label(className := "_label_txt", htmlFor := "tandc")(
              "Accept",
              a(
                href := s"${props.termsUrl}",
                target := "_blank",
                className := "_label_link"
              )(
                "Terms and Conditions"
              )
            )
          )
        )
      ),
      div(className := "div__field_group")(
        div(className := "input__container")(
          input(
            id := "privacyPolicy",
            `type` := "checkbox",
            onChange := (e => setPrivacyPolicy(e.currentTarget.checked))
          ),
          label(className := "_label_txt", htmlFor := "privacyPolicy")(
            "Accept",
            a(
              href := s"${props.privacyPolicyUrl}",
              target := "_blank",
              className := "_label_link"
            )(
              "Privacy Policy Agreement"
            )
          )
        )
      ),
      div(className := "status_container")(
        label(className := "_label_update")(state.message)
      ),
      div(className := "div__field_group")(
        div(
          id := "recoverButton",
          enableButton,
          onClick := { () =>
            recoverWallet()
          }
        )("Recover wallet"),
        if (state.isLoading) {
          mui.CircularProgress
            .variant(indeterminate)
            .size(26)
            .classes(PartialClassNameMapCircul().setRoot("progress_bar"))
        } else {
          div()
        }
      )
    )
  }

  private def recoverWallet(): Unit = {
    if (isValidInput(state)) {
      setState(_.copy(isLoading = true))
      props.backgroundAPI
        .recoverWallet(state.password, Mnemonic(state.seed))
        .onComplete {
          case Success(_) => props.switchToView(Recover)
          case Failure(ex) =>
            setState(_.copy(isLoading = false, message = "Failed recovering wallet"))
            println(s"Failed recovering wallet : ${ex.getMessage}")
        }
    }
  }

  private def isValidInput(state: State): Boolean = {
    if (state.password.isEmpty) {
      setState(_.copy(message = "Password cannot be empty"))
      false
    } else if (state.password != state.password2) {
      setState(_.copy(message = "Password verification does not match"))
      false
    } else if (!Mnemonic.isValid(state.seed)) {
      setState(_.copy(message = "Invalid Seed Phrase"))
      false
    } else {
      setState(_.copy(message = ""))
      true
    }
  }
}
