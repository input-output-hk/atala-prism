package io.iohk.atala.cvp.webextension.popup

import com.alexitc.materialui.facade.materialUiCore.{materialUiCoreStrings, components => mui}
import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import io.iohk.atala.cvp.webextension.common.models.Role
import io.iohk.atala.cvp.webextension.popup.components.{ErrorMessage, ProgressButton}
import io.iohk.atala.cvp.webextension.popup.models.View.{DisplayMnemonic, Welcome}
import io.iohk.atala.cvp.webextension.popup.models.{Data, ImageLogo, View}
import org.scalajs.dom
import org.scalajs.dom.raw._
import slinky.core._
import slinky.core.annotations.react
import slinky.core.facade.ReactElement
import slinky.web.html._
import typings.std.global.console

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.typedarray.{ArrayBuffer, TypedArrayBuffer}
import scala.util.{Failure, Success}

@react class OrganisationDetailsWalletView extends Component {

  case class Props(
      backgroundAPI: BackgroundAPI,
      termsUrl: String,
      privacyPolicyUrl: String,
      data: Data,
      switchToView: (View) => Unit
  )

  case class State(
      orgName: String,
      logo: Option[ImageLogo] = None,
      message: Option[String],
      isLoading: Boolean,
      termsAccepted: Boolean,
      privacyPolicyAccepted: Boolean
  )

  override def initialState: State = {
    State("", None, None, isLoading = false, termsAccepted = false, privacyPolicyAccepted = false)
  }

  override def render(): ReactElement = {

    val logoWarningMessage: ImageLogo => String = logo =>
      s"The logo you are trying to upload has invalid dimensions. " +
        s"Please change your image to match the required upload dimensions and " +
        s"try again. Invalid logo dimensions ${logo.width}px per ${logo.height}px " +
        s"supported logo dimensions must be maximum 50px per 50px."

    def removeImage(): Unit = {
      setState(_.copy(logo = None))
    }

    def enableButton = {
      state.termsAccepted && state.privacyPolicyAccepted &&
      state.logo.fold(true)(_.hasValidDimensions) &&
      !state.isLoading
    }

    div(
      id := "organizationDetails",
      className := "spaceBetween",
      div(
        div(className := "logo_container", id := "logo_container")(
          div(
            className := "div_logo",
            id := "logoPrism",
            img(className := "hola", src := "/assets/images/prism-logo.svg")
          )
        ),
        mui
          .Button(
            div(className := "backArrow", onClick := { () => props.switchToView(DisplayMnemonic) })(
              img(className := "leftArrow", src := "/assets/images/arrow-l.svg"),
              p("Back")
            )
          )
          .className("muiButton")
          .size(materialUiCoreStrings.small),
        h3(className := "h3_register", id := "h3_register", "Account Registration"),
        div(className := "div__field_group")(
          h4(className := "h4_register")("Organization Information")
        ),
        div(className := "")(
          p(
            className := "description",
            id := "description2",
            "Complete the following information. When uploading the logo please it should be maximum 50px per 50px."
          )
        ),
        div(className := "div__field_group")(
          label(className := "_label")("Organization name"),
          div(className := "input__container, bottomPadding")(
            input(
              id := "orgname",
              className := "_input",
              placeholder := "Enter your organization's name",
              value := state.orgName,
              onChange := (e => setOrgName(e.target.value))
            )
          ),
          div(className := s"div__field_group" + s"bottomPadding")(
            div(className := "div__field_group", label(htmlFor := "logo")("Supported files types: png or jpeg")),
            div(className := "input__container, bottomPadding")(
              input(
                className := "inputfile",
                id := "logo",
                `type` := "file",
                accept := "image/png, image/jpeg",
                onChange := (e => setFile(e.target.asInstanceOf[HTMLInputElement].files(0)))
              ),
              label(htmlFor := "logo")("Upload your logo")
            ),
            state.logo.map(logo =>
              if (!logo.hasValidDimensions) {
                div(className := "upload_status_container")(
                  div(
                    className := "flex",
                    p(
                      className := "imgTextError",
                      img(className := "red", src := "/assets/images/paper-clip.svg"),
                      logo.file.name,
                      div(
                        img(src := "/assets/images/x.svg"),
                        className := "logo-img",
                        onClick := { () => removeImage() }
                      )
                    )
                  ),
                  p(
                    logoWarningMessage(logo)
                  )
                )
              } else {
                div(className := "upload_status_container")(logo.file.name)
              }
            )
          ),
          div(className := "")(
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
          state.message.map(ErrorMessage(_))
        )
      ),
      ProgressButton(
        "Register",
        enableButton,
        state.isLoading,
        _ => registerOrganization()
      )
    )
  }

  private def setOrgName(newValue: String): Unit = {
    setState(_.copy(orgName = newValue))
  }

  private def setFile(newValue: File): Unit = {
    val img: HTMLImageElement = dom.document.createElement("img").asInstanceOf[HTMLImageElement]
    img.src = URL.createObjectURL(newValue)
    img.onload = _ => {
      setState(_.copy(logo = Some(ImageLogo(newValue, img.width, img.height))))
    }
  }

  private def setTandC(newValue: Boolean): Unit = {
    setState(_.copy(termsAccepted = newValue))
  }

  private def setPrivacyPolicy(newValue: Boolean): Unit = {
    setState(_.copy(privacyPolicyAccepted = newValue))
  }

  private def registerOrganization(): Unit = {
    if (isValidInput(state)) {
      if (state.logo.isDefined) {
        state.logo.foreach { logo =>
          console.log("file selected");
          val reader = new FileReader()
          reader.onloadend = _ => {
            val buffer = reader.result.asInstanceOf[ArrayBuffer]
            val bb = TypedArrayBuffer.wrap(buffer)
            val arrayBytes: Array[Byte] = new Array[Byte](bb.remaining())
            bb.get(arrayBytes)
            createWallet(arrayBytes)
          }
          reader.readAsArrayBuffer(logo.file)
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
        props.data.password,
        props.data.mnemonic,
        Role.Issuer, // Hardcoded to Issuer till backend when we remove the role
        state.orgName,
        bytes
      )
      .onComplete {
        case Success(_) => props.switchToView(Welcome)
        case Failure(ex) =>
          setState(state.copy(message = Some("Failed creating wallet"), isLoading = false))
          println(s"Failed creating wallet : ${ex.getMessage}")
      }
  }

  private def isValidInput(state: State): Boolean = {
    if (state.orgName.isEmpty) {
      setState(state.copy(message = Some("Organization Name cannot be empty")))
      false
    } else {
      setState(state.copy(message = None))
      true
    }
  }

}
