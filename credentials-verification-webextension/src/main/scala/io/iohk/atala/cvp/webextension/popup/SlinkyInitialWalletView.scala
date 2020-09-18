package io.iohk.atala.cvp.webextension.popup

import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import io.iohk.atala.cvp.webextension.popup.models.View
import io.iohk.atala.cvp.webextension.popup.models.View.{Recover, Register, Unlock}
import slinky.core.StatelessComponent
import slinky.core.annotations.react
import slinky.core.facade.ReactElement
import slinky.web.html._

@react class SlinkyInitialWalletView extends StatelessComponent {
  case class Props(backgroundAPI: BackgroundAPI, switchToView: View => Unit)

  override def render: ReactElement = {
    div(className := "container")(
      div(
        className := "title_container",
        id := "title_container"
      )(
        div(className := "div_logo", id := "logoPrism", img(src := "/assets/images/prism-logo.svg")),
        h3(className := "h3_title", id := "h3_title", "Welcome to your"),
        h1(
          className := "h1_title",
          id := "h1_title",
          "Prism Browser Wallet"
        ),
        p(
          className := "description",
          id := "description",
          "Register now to your browser wallet or recover your account."
        ),
        p(
          className := "div_img",
          id := "div_img",
          img(src := "/assets/images/img-wallet-register.svg")
        )
      ),
      div(
        className := "btn_register",
        id := "registrationScreenButton",
        "Register",
        onClick := { () =>
          props.switchToView(Register)
        }
      ),
      div(
        className := "btn_recover",
        id := "recoveryScreenButton",
        "Recover your account",
        onClick := { () =>
          props.switchToView(Recover)
        }
      )
    )
  }
}
