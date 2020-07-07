package io.iohk.atala.cvp.webextension.popup

import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import io.iohk.atala.cvp.webextension.background.wallet.Role
import io.iohk.atala.cvp.webextension.background.wallet.Role.{Issuer, Verifier}
import io.iohk.atala.cvp.webextension.common.Mnemonic
import io.iohk.atala.cvp.webextension.popup.utils.ValidationUtils
import org.scalajs.dom.html.{Div, Input, Label, Select}
import org.scalajs.dom.raw.FileReader
import scalatags.JsDom.all.{div, label, _}
import typings.std.console

import scala.concurrent.ExecutionContext
import scala.scalajs.js.typedarray.{ArrayBuffer, TypedArrayBuffer}
import scala.util.{Failure, Success}

class RegistrationView(backgroundAPI: BackgroundAPI)(implicit ec: ExecutionContext) {

  def registrationScreen(divElement: Div): Unit = {
    console.info("**************************organisationScreen*****************************")
    val mnemonic = Mnemonic()
    val seedDiv: Div = div(cls := "input__container")(mnemonic.seed).render
    val passwordInput: Input =
      input(id := "password", cls := "_input", `type` := "password", placeholder := "Enter Password").render
    val password2Input: Input =
      input(id := "password2", cls := "_input", `type` := "password", placeholder := "Enter Password Again").render
    val orgNameInput: Input = input(id := "orgname", cls := "_input", placeholder := "Enter Organisation Name").render
    val selectRole: Select = select(id := "role", name := "role")(
      option(value := Issuer.toString)(Issuer.toString),
      option(value := Verifier.toString)(Verifier.toString)
    ).render
    val logoInput: Input = input(id := "logo", `type` := "file", accept := "image/png, image/jpeg").render
    val statusLabel: Label = label(cls := "_label_update")("").render

    val registration = {
      div(id := "registrationScreen")(
        div(cls := "div__field_group")(
          label(cls := "_label")("Seed Phrase: "),
          seedDiv
        ),
        div(cls := "div__field_group")(
          label(cls := "_label")("Password: "),
          div(cls := "input__container")(
            passwordInput
          )
        ),
        div(cls := "div__field_group")(
          label(cls := "_label")("Verify Password: "),
          div(cls := "input__container")(
            password2Input
          )
        ),
        div(cls := "div__field_group")(
          label(cls := "_label")("Organisation Name: "),
          div(cls := "input__container")(
            orgNameInput
          )
        ),
        div(cls := "div__field_group")(
          label(cls := "_label", `for` := "role")("Choose Organisation Role: "),
          div(cls := "input__container")(
            selectRole
          )
        ),
        div(cls := "div__field_group")(
          label(`for` := "logo")("Choose logo image: "),
          div(cls := "input__container")(
            logoInput
          )
        ),
        div(cls := "status_container")(
          div(cls := "input__container")(
            statusLabel
          )
        ),
        div(cls := "div__field_group")(
          div(
            id := "registerButton",
            cls := "div__btn",
            onclick := { () =>
              registerOrganisation(
                seedDiv,
                passwordInput,
                password2Input,
                orgNameInput,
                selectRole,
                logoInput,
                statusLabel,
                divElement
              )
            }
          )("Register")
        )
      )
    }

    divElement.clear()
    divElement.appendChild(registration.render)
  }

  private def registerOrganisation(
      seedDiv: Div,
      passwordInput: Input,
      password2Input: Input,
      orgNameInput: Input,
      selectRole: Select,
      logoInput: Input,
      statusLabel: Label,
      divElement: Div
  ): Unit = {
    ValidationUtils.checkPasswordErrors(passwordInput, password2Input) match {
      case Some(errors) => statusLabel.textContent = errors
      case None =>
        statusLabel.clear()
        val mnemonic = Mnemonic(seedDiv.textContent)

        if (logoInput.files.length != 0) {
          console.log("file selected");
          val reader = new FileReader()
          reader.onloadend = _ => {
            val buffer = reader.result.asInstanceOf[ArrayBuffer]
            val bb = TypedArrayBuffer.wrap(buffer)
            val arrayBytes: Array[Byte] = new Array[Byte](bb.remaining())
            bb.get(arrayBytes)

            backgroundAPI
              .createWallet(
                passwordInput.value,
                mnemonic,
                Role.toRole(selectRole.value),
                orgNameInput.value,
                arrayBytes
              )
              .flatMap { _ =>
                MainWalletView(backgroundAPI).mainWalletScreen(divElement)
              }
              .onComplete {
                case Success(_) => ()
                case Failure(ex) =>
                  println(s"Failed creating wallet : ${ex.getMessage}")
                  throw ex
              }
          }
          reader.readAsArrayBuffer(logoInput.files(0))

        } else {
          console.log("No file selected");
          backgroundAPI
            .createWallet(
              passwordInput.value,
              mnemonic,
              Role.toRole(selectRole.value),
              orgNameInput.value,
              new Array[Byte](0)
            )
            .flatMap { _ =>
              MainWalletView(backgroundAPI).mainWalletScreen(divElement)
            }
            .onComplete {
              case Success(_) => ()
              case Failure(ex) =>
                println(s"Failed creating wallet : ${ex.getMessage}")
                throw ex
            }
        }
    }
  }
}

object RegistrationView {
  def apply(backgroundAPI: BackgroundAPI)(implicit ec: ExecutionContext) = new RegistrationView(backgroundAPI)
}
