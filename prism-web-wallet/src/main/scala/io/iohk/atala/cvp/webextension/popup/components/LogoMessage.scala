package io.iohk.atala.cvp.webextension.popup.components

import io.iohk.atala.cvp.webextension.popup.models.ImageLogo
import slinky.core._
import slinky.core.annotations.react
import slinky.web.html._

@react object LogoMessage {
  case class Props(logo: ImageLogo, removeImage: () => Unit)

  private val logoWarningMessage: ImageLogo => String = logo =>
    s"""The logo you are trying to upload has invalid dimensions 
         |Please change your image to match the required upload dimensions and try again. 
         |Invalid logo dimensions ${logo.width}px per ${logo.height}px supported logo
         | dimensions must be maximum ${logo.maxWidth}px per ${logo.maxHeight}px.""".stripMargin

  val component: FunctionalComponent[Props] = FunctionalComponent[Props] { props =>
    div(className := "upload_status_container")(
      div(
        className := "flex",
        p(
          className := "imgTextError",
          img(className := "red", src := "/assets/images/paper-clip.svg"),
          props.logo.file.name,
          div(
            img(src := "/assets/images/x.svg"),
            className := "logo-img",
            onClick := { () => props.removeImage() }
          )
        )
      ),
      p(
        if (!props.logo.hasValidDimensions) logoWarningMessage(props.logo)
        else ""
      )
    )
  }
}
