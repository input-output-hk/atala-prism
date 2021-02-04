package io.iohk.atala.cvp.webextension.popup

import com.alexitc.materialui.facade.materialUiCore.{materialUiCoreStrings, components => mui}
import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import io.iohk.atala.cvp.webextension.common.Mnemonic
import io.iohk.atala.cvp.webextension.popup.components.{ChipInput, ErrorMessage, ProgressButton}
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
      privacyPolicy: Boolean,
      isLoading: Boolean,
      value: String,
      var values: Seq[String] = Nil
  ) {
    val seed = this.values.mkString(" ")
  }

  override def initialState: State = {
    State("", "", None, false, false, false, "", Nil)
  }

  private def setChips(values: Seq[String]): Unit = {
    setState(_.copy(values = values))
  }

  override def render(): ReactElement = {

    val enableButton = state.tandc && state.privacyPolicy

    div(id := "recoveryScreen", className := "minHeight")(
      div(
        div(className := "div_logo", id := "logoPrism", img(src := "/assets/images/prism-logo.svg")),
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
        ChipInput(chips => setChips(chips)),
        div(className := "")(
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
        div(className := "")(
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
        div(className := "bottomPadding")(
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
        div(className := "")(
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
        )
      ),
      ErrorMessage(state.message),
      ProgressButton(
        "Recover wallet",
        enableButton,
        state.isLoading,
        (isLoading: Boolean) => {
          setState(_.copy(isLoading = isLoading))
          recoverWallet()
        }
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

  private def setPrivacyPolicy(newValue: Boolean): Unit = {
    setState(_.copy(privacyPolicy = newValue))
  }

  private def recoverWallet(): Unit = {
    if (isValidInput(state)) {
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

  private def isValidInput(state: State): Boolean = {
    if (state.password.isEmpty) {
      setState(_.copy(message = Some("Password cannot be empty")))
      false
    } else if (state.password != state.password2) {
      setState(_.copy(message = Some("Password verification does not match")))
      false
    } else if (!Mnemonic.isValid(state.seed)) {
      setState(_.copy(message = Some("Invalid Seed Phrase")))
      false
    } else {
      setState(_.copy(message = None))
      true
    }
  }

}
