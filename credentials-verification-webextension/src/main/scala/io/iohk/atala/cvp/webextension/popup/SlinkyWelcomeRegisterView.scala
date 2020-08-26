package io.iohk.atala.cvp.webextension.popup

import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import io.iohk.atala.cvp.webextension.popup.models.View
import io.iohk.atala.cvp.webextension.popup.models.View.Main
import slinky.core.StatelessComponent
import slinky.core.annotations.react
import slinky.core.facade.ReactElement
import slinky.web.html._

@react class SlinkyWelcomeRegisterView extends StatelessComponent {

  case class Props(backgroundAPI: BackgroundAPI, switchToView: View => Unit)

  override def render: ReactElement = {
    div(id := "welcomeRegisterScreen")(
      div(className := "welcome_img")(
        img(src := "/assets/images/Done.png")
      ),
      p(className := "welcome_registration_text")("Your account has been successfully registered!"),
      div(className := "input__container"),
      div(className := "div__field_group")(
        div(
          id := "nextButton",
          className := "btn_register",
          onClick := { () =>
            props.switchToView(Main)
          }
        )("Login")
      )
    )
  }
}
