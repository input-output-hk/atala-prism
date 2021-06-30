package io.iohk.atala.cvp.webextension.popup

import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import io.iohk.atala.cvp.webextension.popup.models.View
import io.iohk.atala.cvp.webextension.popup.models.View.Main
import slinky.core.StatelessComponent
import slinky.core.annotations.react
import slinky.core.facade.ReactElement
import slinky.web.html._

@react class WelcomeRecoverView extends StatelessComponent {

  case class Props(backgroundAPI: BackgroundAPI, switchToView: (View) => Unit)

  override def render(): ReactElement = {
    div(id := "welcomeRecoveryScreen", className := "generalContainer")(
      div(
        className := "div_logo",
        id := "logoPrism",
        img(className := "logoImage", src := "/assets/images/prism-logo.svg")
      ),
      div(
        className := "elementWrapper",
        div(
          className := "empty-component"
        ),
        div(className := "welcome_img")(
          img(className := "img-size", src := "/assets/images/unlock.png"),
          h3(className := "h3_welcome_back")("Welcome Back"),
          p(className := "welcome_registration_text")("Your wallet has been successfully recovered")
        ),
        div(className := "div__field_group")(
          div(
            id := "nextButton",
            className := "btn_register",
            onClick := { () =>
              props.switchToView(Main)
            }
          )("Next")
        )
      )
    )
  }
}
