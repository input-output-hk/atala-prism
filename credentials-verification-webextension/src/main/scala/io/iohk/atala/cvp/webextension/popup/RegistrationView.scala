package io.iohk.atala.cvp.webextension.popup

import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import io.iohk.atala.cvp.webextension.background.wallet.Role.{Issuer, Verifier}
import io.iohk.atala.cvp.webextension.background.wallet.{Role, WalletManager}
import io.iohk.atala.cvp.webextension.common.Mnemonic
import org.scalajs.dom.html.{Div, Input, Select}
import org.scalajs.dom.raw.{FileReader, HTMLDivElement}
import scalatags.JsDom.all.{div, _}
import typings.std.console

import scala.scalajs.js.typedarray.{ArrayBuffer, TypedArrayBuffer}
class RegistrationView(backgroundAPI: BackgroundAPI) {

  def registrationScreen(divElement: Div): Unit = {
    console.info("**************************organisationScreen*****************************")
    val mnemonic = Mnemonic()
    lazy val seedDiv: Div = div(`class` := "input__container")(mnemonic.seed).render
    lazy val orgNameInput: Input = input(
      `class` := "_input",
      id := "organisationName",
      `type` := "text",
      placeholder := "Enter Organisation Name"
    ).render
    lazy val selectRole: Select = select(id := "role", name := "role")(
      option(value := Issuer.toString)(Issuer.toString),
      option(value := Verifier.toString)(Verifier.toString)
    ).render
    lazy val logoInput: Input =
      input(id := "logo", `type` := "file", accept := "image/png, image/jpeg").render

    lazy val registration = {
      div(
        div(`class` := "div__field_group")(
          label(`class` := "_label")("Seed Phrase: "),
          seedDiv
        ),
        div(`class` := "div__field_group")(
          label(`class` := "_label")("Organisation Name: "),
          div(`class` := "input__container")(
            orgNameInput
          )
        ),
        div(`class` := "div__field_group")(
          label(`class` := "_label", `for` := "role")("Choose Organisation Role: "),
          div(`class` := "input__container")(
            selectRole
          )
        ),
        div(`class` := "div__field_group")(
          label(`for` := "logo")("Choose logo image: "),
          div(`class` := "input__container")(
            logoInput
          )
        ),
        div(`class` := "div__field_group")(
          div(
            `class` := "div__btn",
            id := "registerOrganisation",
            onclick := { () => registerOrganisation(seedDiv, orgNameInput, selectRole, logoInput) }
          )("Register")
        )
      )
    }
    divElement.innerHTML = "" // This is the way I could make it work using scalatags onclick doesnt works
    divElement.appendChild(registration.render)
  }
  private def registerOrganisation(
      seedDiv: Div,
      orgNameInput: Input,
      selectRole: Select,
      logoInput: Input
  ): Unit = {

    val mnemonic = Mnemonic(seedDiv.textContent)

    if (logoInput.files.length != 0) {
      console.log("file selected");
      val reader = new FileReader()
      reader.onloadend = _ => {
        val buffer = reader.result.asInstanceOf[ArrayBuffer]
        val bb = TypedArrayBuffer.wrap(buffer)
        val arrayBytes: Array[Byte] = new Array[Byte](bb.remaining())
        bb.get(arrayBytes)

        backgroundAPI.createWallet(
          WalletManager.FIXME_WALLET_PASSWORD,
          mnemonic,
          Role.toRole(selectRole.value),
          orgNameInput.value,
          arrayBytes
        )
      }
      reader.readAsArrayBuffer(logoInput.files(0))
    } else {
      console.log("No file selected");
      backgroundAPI.createWallet(
        WalletManager.FIXME_WALLET_PASSWORD,
        mnemonic,
        Role.toRole(selectRole.value),
        orgNameInput.value,
        new Array[Byte](0)
      )
    }
  }

}

object RegistrationView {
  def apply(backgroundAPI: BackgroundAPI) = new RegistrationView(backgroundAPI)
}
