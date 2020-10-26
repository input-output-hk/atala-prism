package io.iohk.atala.cvp.webextension.popup

import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import io.iohk.atala.cvp.webextension.common.Mnemonic
import io.iohk.atala.cvp.webextension.common.models.Role
import io.iohk.atala.cvp.webextension.common.models.Role.{Issuer, Verifier}
import io.iohk.atala.cvp.webextension.popup.models.View
import io.iohk.atala.cvp.webextension.popup.models.View.Register
import org.scalajs.dom.raw.{File, FileReader, HTMLInputElement}
import slinky.core._
import slinky.core.annotations.react
import slinky.core.facade.ReactElement
import slinky.web.html._
import typings.materialUiCore.PartialClassNameMapCircul
import typings.materialUiCore.components.CircularProgress
import typings.materialUiCore.materialUiCoreStrings.indeterminate
import typings.std.console

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.typedarray.{ArrayBuffer, TypedArrayBuffer}
import scala.util.{Failure, Success}

@react class SlinkyRegisterWalletView extends Component {
  case class Props(backgroundAPI: BackgroundAPI, termsUrl: String, privacyPolicyUrl: String, switchToView: View => Unit)
  case class State(
      mnemonic: Mnemonic,
      password: String,
      password2: String,
      orgName: String,
      role: Role,
      fileOpt: Option[File] = None,
      message: String,
      isLoading: Boolean = false,
      tandc: Boolean = false,
      privacyPolicy: Boolean = false
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

  private def setOrgName(newValue: String): Unit = {
    setState(_.copy(orgName = newValue))
  }

  private def setRole(newValue: String): Unit = {
    setState(_.copy(role = Role.withName(newValue)))
  }

  private def setFile(newValue: File): Unit = {
    setState(_.copy(fileOpt = Some(newValue)))
  }

  override def initialState: State = {
    State(Mnemonic(), "", "", "", Issuer, None, "")
  }

  private def setTandC(newValue: Boolean): Unit = {
    setState(_.copy(tandc = newValue))
  }

  private def setPrivacyPolicy(newValue: Boolean): Unit = {
    setState(_.copy(privacyPolicy = newValue))
  }

  override def render: ReactElement = {

    def enableButton = {
      if (state.tandc && state.privacyPolicy && !state.isLoading) {
        className := "btn_register"
      } else {
        className := "btn_register disabled"
      }
    }

    div(id := "registrationScreen")(
      h3(className := "h3_register", id := "h3_register", "Wallet registration"),
      div(className := "div__field_group")(
        h4(className := "h4_register")("Save your recovery phrase"),
        p(
          className := "description",
          id := "description",
          "The recovery phrase is the only way to recover your wallet. Please write it down and keep it safe."
        ),
        mnemonicElement
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
              onChange := (e => setPassword2(e.target.value))
            )
          )
        ),
        div(className := "div__field_group")(
          label(className := "_label")("Organization name"),
          div(className := "input__container")(
            input(
              id := "orgname",
              className := "_input",
              placeholder := "Enter your organization's name",
              value := state.orgName,
              onChange := (e => setOrgName(e.target.value))
            )
          )
        ),
        div(className := "div__field_group")(
          label(className := "_label", htmlFor := "role")("Select your Organization Role"),
          div(className := "input__container")(
            select(id := "role", name := "role", onChange := (e => setRole(e.target.value)))(
              option(value := Issuer.entryName)(Issuer.entryName),
              option(value := Verifier.entryName)(Verifier.entryName)
            )
          )
        ),
        div(className := "div__field_group")(
          label(htmlFor := "logo")("Supported files types: png or jpeg"),
          div(className := "input__container")(
            input(
              className := "inputfile",
              id := "logo",
              `type` := "file",
              accept := "image/png, image/jpeg",
              onChange := (e => setFile(e.target.asInstanceOf[HTMLInputElement].files(0)))
            ),
            label(htmlFor := "logo")("Upload your logo")
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
          div(className := "input__container")(
            label(className := "_label_update")(state.message)
          )
        ),
        div(className := "div__field_group")(
          div(
            id := "registerButton",
            enableButton,
            onClick := { () =>
              registerOrganization()
            }
          )("Register"),
          if (state.isLoading) {
            CircularProgress(
              variant = indeterminate,
              size = 26,
              classes = PartialClassNameMapCircul(root = "progress_bar")
            )()
          } else {
            div()
          }
        )
      )
    )
  }

  private def registerOrganization(): Unit = {
    if (isValidInput(state)) {
      if (state.fileOpt.isDefined) {
        state.fileOpt.foreach { file =>
          console.log("file selected");
          val reader = new FileReader()
          reader.onloadend = _ => {
            val buffer = reader.result.asInstanceOf[ArrayBuffer]
            val bb = TypedArrayBuffer.wrap(buffer)
            val arrayBytes: Array[Byte] = new Array[Byte](bb.remaining())
            bb.get(arrayBytes)
            createWallet(arrayBytes)
          }
          reader.readAsArrayBuffer(file)
        }
      } else {
        createWallet(Array.empty[Byte])
      }
    }
  }

  private def createWallet(bytes: Array[Byte]): Unit = {
    setState(state.copy(isLoading = true))
    props.backgroundAPI
      .createWallet(
        state.password,
        state.mnemonic,
        state.role,
        state.orgName,
        bytes
      )
      .onComplete {
        case Success(_) => props.switchToView(Register)
        case Failure(ex) =>
          setState(state.copy(message = "Failed creating wallet", isLoading = false))
          println(s"Failed creating wallet : ${ex.getMessage}")
      }
  }

  private def isValidInput(state: State): Boolean = {
    if (state.password.isEmpty) {
      setState(state.copy(message = "Password cannot be empty"))
      false
    } else if (state.password != state.password2) {
      setState(state.copy(message = "Password verification does not match"))
      false
    } else if (state.orgName.isEmpty) {
      setState(state.copy(message = "Organization Name cannot be empty"))
      false
    } else {
      setState(state.copy(message = ""))
      true
    }
  }
}
