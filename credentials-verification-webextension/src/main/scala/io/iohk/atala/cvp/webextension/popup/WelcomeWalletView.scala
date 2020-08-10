package io.iohk.atala.cvp.webextension.popup

import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import org.scalajs.dom.html.Div
import scalatags.JsDom.all.{div, id, _}

import scala.concurrent.ExecutionContext

class WelcomeWalletView(backgroundAPI: BackgroundAPI)(implicit ec: ExecutionContext) {
  val nextButton = div(cls := "btn_cancel", id := "btn_cancel")("Cancel").render

  def registerWelcomeScreen(divElement: Div) = {

    val welcome = div(id := "welcomeRegisterScreen")(
      div(cls := "welcome_img")(
        img(src := "/assets/images/Done.png")
      ),
      p(cls := "welcome_registration_text")("Your account has been successfully registered!"),
      div(cls := "input__container"),
      div(cls := "div__field_group")(
        div(
          id := "nextButton",
          cls := "btn_register",
          onclick := { () =>
            MainWalletView(backgroundAPI).mainWalletScreen(divElement)
          }
        )("Login")
      )
    )

    divElement.clear()
    divElement.appendChild(welcome.render)

  }

  def recoverWelcomeScreen(divElement: Div) = {
    val welcome = div(id := "welcomeRecoveryScreen")(
      div(cls := "welcome_img")(
        img(src := "/assets/images/unlock.png")
      ),
      h3(cls := "h3_welcome_back")("Welcome Back"),
      p(cls := "welcome_registration_text")("Your account has been successfully recovered"),
      div(cls := "input__container"),
      div(cls := "div__field_group")(
        div(
          id := "nextButton",
          cls := "btn_register",
          onclick := { () =>
            MainWalletView(backgroundAPI).mainWalletScreen(divElement)
          }
        )("Next")
      )
    )
    divElement.clear()
    divElement.appendChild(welcome.render)
  }

}

object WelcomeWalletView {
  def apply(backgroundAPI: BackgroundAPI)(implicit ec: ExecutionContext) = new WelcomeWalletView(backgroundAPI)
}
